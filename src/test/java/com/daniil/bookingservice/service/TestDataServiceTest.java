package com.daniil.bookingservice.service;

import static org.junit.jupiter.api.Assertions.*;

import com.daniil.bookingservice.entity.Booking;
import com.daniil.bookingservice.entity.BookingStatus;
import com.daniil.bookingservice.entity.User;
import com.daniil.bookingservice.repository.BookingRepository;
import com.daniil.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class TestDataServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks // Creates TestDataService and injects the mocks
    private TestDataService testDataService;

    // --- Test Data ---
    private User testUser1;
    private User testUser2;
    private User vipUser;
    private User adminUser;
    private User systemUser;
    private User errorUser;

    @BeforeEach
    void setUp() {
        // User Mocks Setup
        testUser1 = User.builder().id(1L).username("john_doe").password("encoded1").role("USER").build();
        testUser2 = User.builder().id(2L).username("jane_smith").password("encoded2").role("USER").build();
        vipUser = User.builder().id(4L).username("vip_client").password("encodedVip").role("VIP").build();
        adminUser = User.builder().id(5L).username("admin").password("encodedAdmin").role("ADMIN").build();
        systemUser = User.builder().id(6L).username("system_user").password("encodedSystem").role("SYSTEM").build();
        errorUser = User.builder().id(7L).username("test_user").password("encodedTest").role("USER").build();

        // Mocking findByUsername for users needed in booking creation
        // Default mocks for users needed by createTestBookings
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser1));
        when(userRepository.findByUsername("jane_smith")).thenReturn(Optional.of(testUser2));
        when(userRepository.findByUsername("vip_client")).thenReturn(Optional.of(vipUser));
        when(userRepository.findByUsername("test_user")).thenReturn(Optional.of(errorUser));
        // Not mocking 'mike_wilson' initially to test case where user might not be found if needed,
        // but createTestBookings expects it, so we add it for that specific test.
        User mikeUser = User.builder().id(3L).username("mike_wilson").password("encoded3").role("USER").build();
        when(userRepository.findByUsername("mike_wilson")).thenReturn(Optional.of(mikeUser));
    }


    // =====================================================
    //         ТЕСТЫ ДЛЯ МЕТОДА createTestBookings
    // =====================================================

    @Test
    @DisplayName("createTestBookings: Should create bookings if none exist")
    void createTestBookings_NoExistingBookings_ShouldSaveBookings() {
        // Arrange
        given(bookingRepository.count()).willReturn(0L); // Simulate empty repository

        // Act
        testDataService.createTestBookings();

        // Assert & Verify
        verify(bookingRepository, times(2)).count();

        // Verify findByUsername was called for necessary users
        verify(userRepository, times(1)).findByUsername("john_doe");
        verify(userRepository, times(1)).findByUsername("jane_smith");
        verify(userRepository, times(1)).findByUsername("mike_wilson");
        verify(userRepository, times(1)).findByUsername("vip_client");
        verify(userRepository, times(1)).findByUsername("test_user");
        // Admin and system users are not needed for bookings, so their findByUsername shouldn't be called

        // Verify save was called 6 times (for the 6 bookings)
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(6)).save(bookingCaptor.capture());

        // Check properties of saved bookings (example checks)
        var savedBookings = bookingCaptor.getAllValues();
        assertTrue(savedBookings.stream().anyMatch(b -> b.getUserId().equals(testUser1.getId()) && b.getStatus() == BookingStatus.CONFIRMED));
        assertTrue(savedBookings.stream().anyMatch(b -> b.getUserId().equals(testUser2.getId()) && b.getStatus() == BookingStatus.PENDING));
        assertTrue(savedBookings.stream().anyMatch(b -> b.getUserId().equals(3L) /* mikeId */ && b.getStatus() == BookingStatus.CANCELLED && b.getRoomId() == null));
        assertTrue(savedBookings.stream().anyMatch(b -> b.getUserId().equals(vipUser.getId()) && b.getStatus() == BookingStatus.CONFIRMED && b.getRoomId() == 201L));
        assertTrue(savedBookings.stream().anyMatch(b -> b.getUserId().equals(testUser2.getId()) && b.getStartDate().isBefore(LocalDate.now()) && b.getStatus() == BookingStatus.CONFIRMED)); // Completed booking check

        // Check correlationId format basic structure
        assertTrue(savedBookings.stream().anyMatch(b -> b.getCorrelationId() != null && b.getCorrelationId().startsWith("booking-confirmed-")));
        assertTrue(savedBookings.stream().anyMatch(b -> b.getCorrelationId() != null && b.getCorrelationId().startsWith("booking-pending-")));

        // Verify no other repository methods were called
        verifyNoMoreInteractions(bookingRepository);
        verifyNoMoreInteractions(userRepository); // All expected user lookups were called
    }

}