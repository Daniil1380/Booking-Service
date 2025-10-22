package com.daniil.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для регистрации / авторизации и административного CRUD.
 * password присутствует для передачи при регистрации/аутентификации,
 * но никогда не должен возвращаться в ответах.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100)
    private String password;

    /**
     * ROLE_USER или ROLE_ADMIN (или просто "USER"/"ADMIN" в зависимости от вашей конвенции).
     * При регистрации обычно не передаётся — сервер ставит USER по умолчанию.
     */
    private String role;
}

