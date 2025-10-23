package com.daniil.bookingservice;


import com.daniil.bookingservice.service.TestDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final TestDataService testDataService;

    @Override
    public void run(String... args) {
        log.info("Initializing test data...");

        testDataService.createTestUsers();
        testDataService.createTestBookings();
        testDataService.createIdempotencyTestData();
        testDataService.createHistoricalData();
        testDataService.createEdgeCaseData();

        log.info("Test data initialization completed");
    }
}
