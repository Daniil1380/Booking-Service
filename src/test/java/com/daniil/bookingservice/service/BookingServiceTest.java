package com.daniil.bookingservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.daniil.bookingservice.dto.BookingRequest;
import com.daniil.bookingservice.entity.Booking;
import com.daniil.bookingservice.entity.BookingStatus;
import com.daniil.bookingservice.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @InjectMocks
    private BookingService bookingService;

    private BookingRequest bookingRequest;
    private final Long userId = 1L;
    private final String correlationId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        bookingRequest = new BookingRequest();
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3));
        bookingRequest.setCorrelationId(correlationId);
    }

    @Test
    void createBooking_WithNewRequest_ReturnsConfirmedBooking() {
        // Arrange
        Long roomId = 101L;
        when(bookingRepository.findByCorrelationId(anyString())).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), any())).thenReturn(roomId);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.createBooking(bookingRequest, userId);

        // Assert
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(roomId, result.getRoomId());
        assertEquals(userId, result.getUserId());
        verify(restTemplate).getForObject(anyString(), any());
        verify(restTemplate).postForEntity(anyString(), isNull(), any());
    }

    @Test
    void createBooking_WithExistingCorrelationId_ReturnsExistingBooking() {
        // Arrange
        Booking existingBooking = Booking.builder().id(1L).correlationId(correlationId).build();
        when(bookingRepository.findByCorrelationId(anyString())).thenReturn(Optional.of(existingBooking));

        // Act
        Booking result = bookingService.createBooking(bookingRequest, userId);

        // Assert
        assertEquals(existingBooking, result);
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void createBooking_WhenNoRoomsAvailable_ReturnsCancelledBooking() {
        // Arrange
        when(bookingRepository.findByCorrelationId(anyString())).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.createBooking(bookingRequest, userId);

        // Assert
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertNull(result.getRoomId());
        verify(restTemplate).getForObject(anyString(), any());
        verify(restTemplate, never()).postForEntity(anyString(), isNull(), any());
    }


    @Test
    void createBooking_WhenCompensationFails_StillReturnsCancelled() {
        // Arrange
        Long roomId = 101L;
        when(bookingRepository.findByCorrelationId(anyString())).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), any())).thenReturn(roomId);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.postForEntity(anyString(), isNull(), any()))
                .thenThrow(new RestClientException("Service unavailable"))
                .thenThrow(new RestClientException("Compensation failed"));

        // Act
        Booking result = bookingService.createBooking(bookingRequest, userId);

        // Assert
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(restTemplate, times(2)).postForEntity(anyString(), isNull(), any());
    }

    @Test
    void createBooking_WithoutCorrelationId_GeneratesNewOne() {
        // Arrange
        bookingRequest.setCorrelationId(null);
        Long roomId = 101L;
        when(bookingRepository.findByCorrelationId(anyString())).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), any())).thenReturn(roomId);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.createBooking(bookingRequest, userId);

        // Assert
        assertNotNull(result.getCorrelationId());
    }


    @Test
    void getBooking_WithExistingId_ReturnsBooking() {
        // Arrange
        Long bookingId = 1L;
        Booking expectedBooking = Booking.builder().id(bookingId).build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(expectedBooking));

        // Act
        Optional<Booking> result = bookingService.getBooking(bookingId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(expectedBooking, result.get());
    }

    @Test
    void getBooking_WithNonExistingId_ReturnsEmpty() {
        // Arrange
        Long bookingId = 999L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act
        Optional<Booking> result = bookingService.getBooking(bookingId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void saveBooking_SetsAllFieldsCorrectly() {
        // Arrange
        Long roomId = 101L;
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.saveBooking(userId, roomId, bookingRequest, BookingStatus.PENDING, correlationId);

        // Assert
        assertEquals(userId, result.getUserId());
        assertEquals(roomId, result.getRoomId());
        assertEquals(bookingRequest.getStartDate(), result.getStartDate());
        assertEquals(bookingRequest.getEndDate(), result.getEndDate());
        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertEquals(correlationId, result.getCorrelationId());
        assertNotNull(result.getCreatedAt());
    }
}