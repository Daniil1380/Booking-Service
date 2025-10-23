package com.daniil.bookingservice.controller;

import com.daniil.bookingservice.dto.UserDto;
import com.daniil.bookingservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_InvalidUser_ShouldReturnForbidden() throws Exception {
        UserDto invalid = UserDto.builder()
                .username("")
                .password("123")
                .build();

        mockMvc.perform(post("/api/user/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isForbidden());
    }

    @Test
    void auth_InvalidUser_ShouldReturnForbidden() throws Exception {
        UserDto invalid = UserDto.builder()
                .username("a") // слишком короткий
                .password("")
                .build();

        mockMvc.perform(post("/api/user/auth")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isForbidden());
    }
}
