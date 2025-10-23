package com.daniil.bookingservice.service;

import com.daniil.bookingservice.entity.Booking;
import com.daniil.bookingservice.entity.BookingStatus;
import com.daniil.bookingservice.entity.User;
import com.daniil.bookingservice.repository.BookingRepository;
import com.daniil.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public void createTestUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already exist, skipping user creation");
            return;
        }

        // Обычные пользователи для тестирования
        userRepository.save(User.builder()
                .username("john_doe")
                .password("password123")
                .role("USER")
                .build());

        userRepository.save(User.builder()
                .username("jane_smith")
                .password("password123")
                .role("USER")
                .build());

        userRepository.save(User.builder()
                .username("mike_wilson")
                .password("password123")
                .role("USER")
                .build());

        // VIP пользователь для тестирования приоритета
        userRepository.save(User.builder()
                .username("vip_client")
                .password("vippass456")
                .role("VIP")
                .build());

        // Администратор для управления системой
        userRepository.save(User.builder()
                .username("admin")
                .password("admin789")
                .role("ADMIN")
                .build());

        // Системный пользователь для внутренних операций
        userRepository.save(User.builder()
                .username("system_user")
                .password("systempass")
                .role("SYSTEM")
                .build());

        // Тестовый пользователь для демонстрации ошибок
        userRepository.save(User.builder()
                .username("test_user")
                .password("testpass")
                .role("USER")
                .build());

        log.info("Created {} test users", userRepository.count());
    }

    @Transactional
    public void createTestBookings() {
        if (bookingRepository.count() > 0) {
            log.info("Bookings already exist, skipping booking creation");
            return;
        }

        Long johnId = userRepository.findByUsername("john_doe").get().getId();
        Long janeId = userRepository.findByUsername("jane_smith").get().getId();
        Long mikeId = userRepository.findByUsername("mike_wilson").get().getId();
        Long vipId = userRepository.findByUsername("vip_client").get().getId();
        Long testUserId = userRepository.findByUsername("test_user").get().getId();

        // 1. Успешное подтвержденное бронирование (демонстрация успешного flow)
        bookingRepository.save(Booking.builder()
                .userId(johnId)
                .roomId(101L)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(10))
                .status(BookingStatus.CONFIRMED)
                .correlationId("booking-confirmed-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        // 2. Ожидающее бронирование (демонстрация pending state)
        bookingRepository.save(Booking.builder()
                .userId(janeId)
                .roomId(102L)
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusDays(17))
                .status(BookingStatus.PENDING)
                .correlationId("booking-pending-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build());

        // 3. Отмененное бронирование (демонстрация compensation/circuit breaker)
        bookingRepository.save(Booking.builder()
                .userId(mikeId)
                .roomId(null) // Комната не была выделена - сценарий отказа hotel-service
                .startDate(LocalDate.now().plusDays(21))
                .endDate(LocalDate.now().plusDays(24))
                .status(BookingStatus.CANCELLED)
                .correlationId("booking-cancelled-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusHours(6))
                .build());

        // 4. VIP бронирование (демонстрация бизнес-логики для VIP)
        bookingRepository.save(Booking.builder()
                .userId(vipId)
                .roomId(201L) // VIP номер
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(8))
                .status(BookingStatus.CONFIRMED)
                .correlationId("vip-booking-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusHours(12))
                .build());

        // 5. Завершенное бронирование (historical data)
        bookingRepository.save(Booking.builder()
                .userId(janeId)
                .roomId(103L)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .status(BookingStatus.CONFIRMED)
                .correlationId("booking-completed-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusDays(7))
                .build());

        // 6. Бронирование на сегодня (edge case - same day booking)
        bookingRepository.save(Booking.builder()
                .userId(testUserId)
                .roomId(104L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .status(BookingStatus.CONFIRMED)
                .correlationId("same-day-booking-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());

        log.info("Created {} test bookings", bookingRepository.count());
    }

    @Transactional
    public void createIdempotencyTestData() {
        Long johnId = userRepository.findByUsername("john_doe").get().getId();
        Long janeId = userRepository.findByUsername("jane_smith").get().getId();

        // Известные correlationId для тестирования идемпотентности
        String[] testCorrelationIds = {
                "idempotency-test-12345",
                "idempotency-test-67890",
                "duplicate-request-test"
        };

        for (int i = 0; i < testCorrelationIds.length; i++) {
            bookingRepository.save(Booking.builder()
                    .userId(i % 2 == 0 ? johnId : janeId)
                    .roomId(150L + i)
                    .startDate(LocalDate.now().plusDays(30 + i))
                    .endDate(LocalDate.now().plusDays(33 + i))
                    .status(BookingStatus.CONFIRMED)
                    .correlationId(testCorrelationIds[i])
                    .createdAt(LocalDateTime.now().minusHours(i + 1))
                    .build());
        }

        log.info("Created idempotency test data with correlationIds: {}", String.join(", ", testCorrelationIds));
    }

    @Transactional
    public void createHistoricalData() {
        Long janeId = userRepository.findByUsername("jane_smith").get().getId();
        Long vipId = userRepository.findByUsername("vip_client").get().getId();
        Long mikeId = userRepository.findByUsername("mike_wilson").get().getId();

        // Исторические успешные бронирования (для аналитики)
        for (int i = 1; i <= 10; i++) {
            bookingRepository.save(Booking.builder()
                    .userId(i % 3 == 0 ? vipId : (i % 2 == 0 ? janeId : mikeId))
                    .roomId(200L + i)
                    .startDate(LocalDate.now().minusMonths(i).plusDays(1))
                    .endDate(LocalDate.now().minusMonths(i).plusDays(4))
                    .status(BookingStatus.CONFIRMED)
                    .correlationId("historical-" + i + "-" + UUID.randomUUID().toString().substring(0, 6))
                    .createdAt(LocalDateTime.now().minusMonths(i))
                    .build());
        }

        // Статистика отмен (для демонстрации compensation logic)
        for (int i = 1; i <= 5; i++) {
            bookingRepository.save(Booking.builder()
                    .userId(mikeId)
                    .roomId(null) // Не удалось выделить комнату
                    .startDate(LocalDate.now().minusMonths(i).plusDays(10))
                    .endDate(LocalDate.now().minusMonths(i).plusDays(13))
                    .status(BookingStatus.CANCELLED)
                    .correlationId("cancelled-" + i + "-" + UUID.randomUUID().toString().substring(0, 6))
                    .createdAt(LocalDateTime.now().minusMonths(i).minusDays(5))
                    .build());
        }

        // Pending бронирования старше 24 часов (для демонстрации cleanup jobs)
        bookingRepository.save(Booking.builder()
                .userId(janeId)
                .roomId(301L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(8))
                .status(BookingStatus.PENDING)
                .correlationId("old-pending-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusHours(36))
                .build());

        log.info("Created historical data for analytics and cleanup scenarios");
    }

    @Transactional
    public void createEdgeCaseData() {
        Long johnId = userRepository.findByUsername("john_doe").get().getId();
        Long testUserId = userRepository.findByUsername("test_user").get().getId();
        Long vipId = userRepository.findByUsername("vip_client").get().getId();

        // 1. Очень длительное бронирование (30 дней)
        bookingRepository.save(Booking.builder()
                .userId(vipId)
                .roomId(500L)
                .startDate(LocalDate.now().plusDays(60))
                .endDate(LocalDate.now().plusDays(90))
                .status(BookingStatus.CONFIRMED)
                .correlationId("long-stay-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now())
                .build());

        // 2. Бронирование на год вперед
        bookingRepository.save(Booking.builder()
                .userId(johnId)
                .roomId(501L)
                .startDate(LocalDate.now().plusYears(1))
                .endDate(LocalDate.now().plusYears(1).plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .correlationId("future-booking-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now())
                .build());

        // 3. Множественные быстрые запросы (имитация нагрузки)
        for (int i = 0; i < 7; i++) {
            BookingStatus status = switch (i % 3) {
                case 0 -> BookingStatus.CONFIRMED;
                case 1 -> BookingStatus.PENDING;
                default -> BookingStatus.CANCELLED;
            };

            bookingRepository.save(Booking.builder()
                    .userId(testUserId)
                    .roomId(i < 4 ? 600L + i : null) // Половина без номеров
                    .startDate(LocalDate.now().plusDays(45 + i))
                    .endDate(LocalDate.now().plusDays(48 + i))
                    .status(status)
                    .correlationId("load-test-" + i + "-" + UUID.randomUUID().toString().substring(0, 6))
                    .createdAt(LocalDateTime.now().minusMinutes(i * 5L))
                    .build());
        }

        // 4. Overlapping dates для одного пользователя (бизнес-правило)
        bookingRepository.save(Booking.builder()
                .userId(testUserId)
                .roomId(700L)
                .startDate(LocalDate.now().plusDays(15))
                .endDate(LocalDate.now().plusDays(18))
                .status(BookingStatus.CONFIRMED)
                .correlationId("overlap-1-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        bookingRepository.save(Booking.builder()
                .userId(testUserId)
                .roomId(null) // Должно быть отменено из-за пересечения
                .startDate(LocalDate.now().plusDays(17))
                .endDate(LocalDate.now().plusDays(20))
                .status(BookingStatus.CANCELLED)
                .correlationId("overlap-2-" + UUID.randomUUID().toString().substring(0, 8))
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());

        log.info("Created edge case test data for comprehensive testing");
    }

    // Дополнительный метод для создания данных circuit breaker тестов
    @Transactional
    public void createCircuitBreakerTestData() {
        Long testUserId = userRepository.findByUsername("test_user").get().getId();

        // Создаем несколько отмененных бронирований подряд (имитация сбоев hotel-service)
        for (int i = 0; i < 5; i++) {
            bookingRepository.save(Booking.builder()
                    .userId(testUserId)
                    .roomId(null) // Hotel service недоступен
                    .startDate(LocalDate.now().plusDays(100 + i))
                    .endDate(LocalDate.now().plusDays(103 + i))
                    .status(BookingStatus.CANCELLED)
                    .correlationId("circuit-breaker-test-" + i)
                    .createdAt(LocalDateTime.now().minusMinutes(i * 2L))
                    .build());
        }

        log.info("Created circuit breaker test scenarios");
    }
}
