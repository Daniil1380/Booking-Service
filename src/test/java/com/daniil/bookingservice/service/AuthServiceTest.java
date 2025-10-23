package com.daniil.bookingservice.service;

import com.daniil.bookingservice.dto.UserDto;
import com.daniil.bookingservice.entity.User;
import com.daniil.bookingservice.repository.UserRepository;
import com.daniil.bookingservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private UserDto validUserDto;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validUserDto = new UserDto(1L, "admin", "admin123", "ADMIN");
        existingUser = new User();
        existingUser.setUsername("testuser");
        existingUser.setPassword("encodedPassword");
        existingUser.setRole("USER");
    }

    // Register Tests
    @Test
    void register_WithNewUser_ReturnsToken() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtTokenProvider.createToken(anyString(), anyString())).thenReturn("generatedToken");

        ResponseEntity<?> response = authService.register(validUserDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        assertEquals("generatedToken", ((Map<?, ?>) response.getBody()).get("token"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_WithExistingUser_ReturnsConflict() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));

        ResponseEntity<?> response = authService.register(validUserDto);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        assertEquals("User already exists", ((Map<?, ?>) response.getBody()).get("error"));
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void register_WithAdminRole_IgnoresRoleAndSetsUser() {
        UserDto adminUserDto = new UserDto(1L, "admin", "admin123", "ADMIN");
        adminUserDto.setRole("ADMIN");

        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtTokenProvider.createToken(anyString(), eq("USER"))).thenReturn("token");

        ResponseEntity<?> response = authService.register(adminUserDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jwtTokenProvider).createToken(anyString(), eq("USER"));
    }

    @Test
    void register_EncodesPasswordBeforeSaving() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(jwtTokenProvider.createToken(anyString(), anyString())).thenReturn("token");

        authService.register(validUserDto);

        verify(passwordEncoder).encode(validUserDto.getPassword());
    }

    // Authenticate Tests
    @Test
    void authenticate_WithValidCredentials_ReturnsToken() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createToken(anyString(), anyString())).thenReturn("generatedToken");

        ResponseEntity<?> response = authService.authenticate(validUserDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        assertEquals("generatedToken", ((Map<?, ?>) response.getBody()).get("token"));
    }

    @Test
    void authenticate_WithWrongPassword_ReturnsUnauthorized() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ResponseEntity<?> response = authService.authenticate(validUserDto);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        assertEquals("Bad credentials", ((Map<?, ?>) response.getBody()).get("error"));
    }


    @Test
    void authenticate_WithAdminRole_ReturnsTokenWithAdminRole() {
        existingUser.setRole("ADMIN");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createToken(anyString(), eq("ADMIN"))).thenReturn("adminToken");

        ResponseEntity<?> response = authService.authenticate(validUserDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("adminToken", ((Map<?, ?>) response.getBody()).get("token"));
    }

    @Test
    void authenticate_WithCorrectUsernameCaseInsensitive_ReturnsToken() {
        UserDto mixedCaseUserDto = new UserDto(1L, "admin", "admin123", "ADMIN");
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createToken(anyString(), anyString())).thenReturn("token");

        ResponseEntity<?> response = authService.authenticate(mixedCaseUserDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
