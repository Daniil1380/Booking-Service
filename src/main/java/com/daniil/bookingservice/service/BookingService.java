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
        String correlationId = UUID.randomUUID().toString();
        log.info("[{}] Starting booking for roomId={} from {} to {}", correlationId, request.getRoomId(), request.getStartDate(), request.getEndDate());

        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(request.getRoomId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .correlationId(correlationId)
                .build();
        bookingRepository.save(booking);

        String confirmUrl = HOTEL_SERVICE + "/api/rooms/" + request.getRoomId() + "/confirm-availability";
        log.info("[{}] Sending confirm-availability request to {}", correlationId, confirmUrl);

        boolean confirmed = false;
        int retries = 3;
        long backoff = 1000;

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                restTemplate.postForEntity(confirmUrl, null, Void.class);
                confirmed = true;
                log.info("[{}] Room confirmed successfully on attempt {}", correlationId, attempt);
                break;
            } catch (RestClientException ex) {
                log.warn("[{}] Attempt {} failed: {}", correlationId, attempt, ex.getMessage());
                try {
                    Thread.sleep(backoff);
                    backoff *= 2;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (confirmed) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("[{}] Booking CONFIRMED", correlationId);
        } else {
            log.error("[{}] All retry attempts failed. Triggering compensation...", correlationId);
            performCompensation(request.getRoomId(), booking, correlationId);
        }

        return booking;
    }

    private Booking fallbackCreateBooking(BookingRequest request, Long userId, Throwable ex) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] CircuitBreaker OPEN — Hotel service unavailable. Booking CANCELLED immediately: {}", correlationId, ex.getMessage());

        Booking failedBooking = Booking.builder()
                .userId(userId)
                .roomId(request.getRoomId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(BookingStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .correlationId(correlationId)
                .build();
        return bookingRepository.save(failedBooking);
    }

    private void performCompensation(Long roomId, Booking booking, String correlationId) {
        String releaseUrl = HOTEL_SERVICE + "/api/rooms/" + roomId + "/release";
        try {
            restTemplate.postForEntity(releaseUrl, null, Void.class);
            log.info("[{}] Compensation: room released successfully", correlationId);
        } catch (RestClientException e) {
            log.error("[{}] Compensation failed: {}", correlationId, e.getMessage());
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("[{}] Booking CANCELLED", correlationId);
    }

    public Optional<Booking> getBooking(Long id) {
        return bookingRepository.findById(id);
    }
}
