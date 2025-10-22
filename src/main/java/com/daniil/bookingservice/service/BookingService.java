package com.daniil.bookingservice.service;

import com.daniil.bookingservice.dto.BookingRequest;
import com.daniil.bookingservice.entity.Booking;
import com.daniil.bookingservice.entity.BookingStatus;
import com.daniil.bookingservice.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.daniil.bookingservice.dto.BookingRequest;
import com.daniil.bookingservice.entity.Booking;
import com.daniil.bookingservice.entity.BookingStatus;
import com.daniil.bookingservice.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RestTemplate restTemplate;
    private static final String HOTEL_SERVICE = "http://hotel-service";

    @Transactional
    @CircuitBreaker(name = "hotelServiceCB", fallbackMethod = "fallbackCreateBooking")
    public Booking createBooking(BookingRequest request, Long userId) {
        String correlationId = request.getCorrelationId() != null
                ? request.getCorrelationId()
                : UUID.randomUUID().toString();

        // Идемпотентность
        Optional<Booking> existing = bookingRepository.findByCorrelationId(correlationId);
        if (existing.isPresent()) {
            log.info("[{}] Booking already exists -> id={}", correlationId, existing.get().getId());
            return existing.get();
        }

        log.info("[{}] Starting booking from {} to {}", correlationId, request.getStartDate(), request.getEndDate());

        // Запрашиваем оптимальный номер
        Long allocatedRoomId = restTemplate.getForObject(HOTEL_SERVICE + "/api/rooms/allocate", Long.class);
        if (allocatedRoomId == null) {
            log.error("[{}] No rooms available", correlationId);
            return saveBooking(userId, null, request, BookingStatus.CANCELLED, correlationId);
        }

        log.info("[{}] Allocated roomId={}", correlationId, allocatedRoomId);
        Booking booking = saveBooking(userId, allocatedRoomId, request, BookingStatus.PENDING, correlationId);

        try {
            restTemplate.postForEntity(HOTEL_SERVICE + "/api/rooms/" + allocatedRoomId + "/confirm", null, Void.class);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("[{}] Booking confirmed successfully", correlationId);
        } catch (Exception ex) {
            log.error("[{}] Confirm failed: {}", correlationId, ex.getMessage());
            performCompensation(allocatedRoomId, booking, correlationId);
        }

        return booking;
    }

    private Booking saveBooking(Long userId, Long roomId, BookingRequest request, BookingStatus status, String correlationId) {
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(roomId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(status)
                .createdAt(LocalDateTime.now())
                .correlationId(correlationId)
                .build();
        return bookingRepository.save(booking);
    }

    private Booking fallbackCreateBooking(BookingRequest request, Long userId, Throwable ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] CircuitBreaker OPEN — booking cancelled: {}", correlationId, ex.getMessage());
        return saveBooking(userId, null, request, BookingStatus.CANCELLED, correlationId);
    }

    private void performCompensation(Long roomId, Booking booking, String correlationId) {
        try {
            restTemplate.postForEntity(HOTEL_SERVICE + "/api/rooms/" + roomId + "/release", null, Void.class);
            log.info("[{}] Room released successfully", correlationId);
        } catch (Exception e) {
            log.error("[{}] Compensation failed: {}", correlationId, e.getMessage());
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public Optional<Booking> getBooking(Long id) {
        return bookingRepository.findById(id);
    }
}


