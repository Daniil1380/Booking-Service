package com.daniil.bookingservice.dto;

import lombok.Data;

import java.time.LocalDate;


@Data
public class BookingRequest {
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
}

