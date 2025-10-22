package com.daniil.bookingservice.repository;

import com.daniil.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Идемпотентность: поиск бронирования по correlationId
     */
    Optional<Booking> findByCorrelationId(String correlationId);
}

