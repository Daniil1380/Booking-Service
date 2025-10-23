package com.daniil.bookingservice.dto;

import jakarta.validation.constraints.FutureOrPresent;
import lombok.Data;

import java.time.LocalDate;


@Data
public class BookingRequest {
    private Long roomId;
    @FutureOrPresent(message = "startDate must be today or in the future")
    private LocalDate startDate;
    @FutureOrPresent(message = "startDate must be today or in the future")
    private LocalDate endDate;
    private String correlationId;
}

