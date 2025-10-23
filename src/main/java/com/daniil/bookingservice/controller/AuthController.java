package com.daniil.bookingservice.controller;

import com.daniil.bookingservice.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daniil.bookingservice.service.AuthService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto request) {
        return authService.register(request);
    }

    @PostMapping("/auth")
    public ResponseEntity<?> auth(@RequestBody UserDto request) {
        return authService.authenticate(request);
    }
}


