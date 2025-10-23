package com.daniil.bookingservice.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.daniil.bookingservice.dto.BookingRequest;
import com.daniil.bookingservice.entity.Booking;
import com.daniil.bookingservice.entity.BookingStatus;
import com.daniil.bookingservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BookingController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class
})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    private BookingRequest bookingRequest;
    private Booking createdBooking;
    private final Long testBookingId = 10L;
    private final Long hardcodedUserId = 1L;

    @BeforeEach
    void setUp() {
        // Initialize common objects for tests
        bookingRequest = new BookingRequest();
        bookingRequest.setStartDate(LocalDate.now().plusDays(1));
        bookingRequest.setEndDate(LocalDate.now().plusDays(3));
        bookingRequest.setCorrelationId(UUID.randomUUID().toString());

        // Initialize a sample created booking object
        createdBooking = Booking.builder()
                .id(testBookingId)
                .userId(hardcodedUserId) // Matches the controller's hardcoded value
                .roomId(101L)
                .startDate(bookingRequest.getStartDate())
                .endDate(bookingRequest.getEndDate())
                .status(BookingStatus.PENDING) // Example status
                .createdAt(LocalDateTime.now())
                .correlationId(bookingRequest.getCorrelationId())
                .build();
    }

    // =====================================================
    //           ТЕСТЫ ДЛЯ ЭНДПОИНТА POST /api/bookings
    // =====================================================

    @Test
    @DisplayName("POST /api/bookings: Should create booking and return 200 OK with booking details")
    void createBooking_Success_ShouldReturnOkAndBooking() throws Exception {
        // Arrange
        // Mock the service to return the created booking when called
        given(bookingService.createBooking(eq(bookingRequest), eq(hardcodedUserId)))
                .willReturn(createdBooking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // Expect JSON response
                .andExpect(jsonPath("$.id").value(createdBooking.getId())) // Check specific fields in the JSON body
                .andExpect(jsonPath("$.userId").value(createdBooking.getUserId()))
                .andExpect(jsonPath("$.roomId").value(createdBooking.getRoomId()))
                .andExpect(jsonPath("$.status").value(createdBooking.getStatus().toString()));

        // Verify that the service method was called exactly once with the correct arguments
        verify(bookingService, times(1)).createBooking(eq(bookingRequest), eq(hardcodedUserId));
        verifyNoMoreInteractions(bookingService); // Ensure no other unexpected calls were made
    }

    @Test
    @DisplayName("POST /api/bookings: Should handle service returning a PENDING booking")
    void createBooking_ServiceReturnsPending_ShouldReturnOkAndPendingBooking() throws Exception {
        // Arrange
        Booking pendingBooking = createdBooking.builder().status(BookingStatus.PENDING).build(); // Create a pending version
        given(bookingService.createBooking(eq(bookingRequest), eq(hardcodedUserId)))
                .willReturn(pendingBooking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(BookingStatus.PENDING.toString()));

        // Verify
        verify(bookingService, times(1)).createBooking(eq(bookingRequest), eq(hardcodedUserId));
    }

    @Test
    @DisplayName("POST /api/bookings: Should handle service returning a CANCELLED booking")
    void createBooking_ServiceReturnsCancelled_ShouldReturnOkAndCancelledBooking() throws Exception {
        // Arrange
        Booking cancelledBooking = createdBooking.builder().status(BookingStatus.CANCELLED).roomId(null).build(); // Simulate cancelled state
        given(bookingService.createBooking(eq(bookingRequest), eq(hardcodedUserId)))
                .willReturn(cancelledBooking);

        // Act & Assert
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(BookingStatus.CANCELLED.toString()))
                .andExpect(jsonPath("$.roomId").doesNotExist()); // roomId should not be present or null

        // Verify
        verify(bookingService, times(1)).createBooking(eq(bookingRequest), eq(hardcodedUserId));
    }


    @Test
    @DisplayName("POST /api/bookings: Should handle service throwing an unexpected exception")
    void createBooking_ServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        // Mock the service to throw an exception (e.g., RuntimeException)
        RuntimeException serviceException = new RuntimeException("Database error during booking creation");
        given(bookingService.createBooking(eq(bookingRequest), eq(hardcodedUserId)))
                .willThrow(serviceException);

        // Act & Assert
        // Spring Boot's default exception handling for uncaught RuntimeExceptions returns 500
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isInternalServerError()); // Expect HTTP 500

        // Verify
        verify(bookingService, times(1)).createBooking(eq(bookingRequest), eq(hardcodedUserId));
    }


    // =====================================================
    //           ТЕСТЫ ДЛЯ ЭНДПОИНТА GET /api/bookings/{id}
    // =====================================================

    @Test
    @DisplayName("GET /api/bookings/{id}: Should return 200 OK with booking details if found")
    void getBooking_Found_ShouldReturnOkAndBooking() throws Exception {
        // Arrange
        Long bookingIdToFind = 10L;
        // Ensure createdBooking matches the ID we're looking for
        Booking foundBooking = createdBooking.builder().id(bookingIdToFind).build();

        // Mock the service to return the booking wrapped in Optional
        given(bookingService.getBooking(bookingIdToFind))
                .willReturn(Optional.of(foundBooking));

        // Act & Assert
        mockMvc.perform(get("/api/bookings/{id}", bookingIdToFind)
                        .accept(MediaType.APPLICATION_JSON)) // Specify acceptable response type
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(bookingIdToFind))
                .andExpect(jsonPath("$.userId").value(foundBooking.getUserId()));

        // Verify
        verify(bookingService, times(1)).getBooking(bookingIdToFind);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    @DisplayName("GET /api/bookings/{id}: Should return 404 Not Found if booking is not found")
    void getBooking_NotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        Long bookingIdToFind = 99L; // An ID that doesn't exist

        // Mock the service to return an empty Optional
        given(bookingService.getBooking(bookingIdToFind))
                .willReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/bookings/{id}", bookingIdToFind)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // Expect HTTP 404 Not Found

        // Verify
        verify(bookingService, times(1)).getBooking(bookingIdToFind);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    @DisplayName("GET /api/bookings/{id}: Should handle service throwing an unexpected exception")
    void getBooking_ServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        Long bookingIdToFind = 10L;
        RuntimeException serviceException = new RuntimeException("Error fetching booking from DB");
        given(bookingService.getBooking(bookingIdToFind))
                .willThrow(serviceException);

        // Act & Assert
        mockMvc.perform(get("/api/bookings/{id}", bookingIdToFind)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError()); // Expect HTTP 500

        // Verify
        verify(bookingService, times(1)).getBooking(bookingIdToFind);
    }

}