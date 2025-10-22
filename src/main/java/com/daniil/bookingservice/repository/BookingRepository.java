package com.daniil.bookingservice.repository;

import com.daniil.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}

