package ru.vspochernin.errapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на регистрацию")
public record RegisterRequest(

        @Schema(description = "Логин", example = "new_user")
        @NotBlank
        @Size(min = 3, max = 64)
        String login,

        @Schema(description = "Email", example = "newuser@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Пароль", example = "new_user_password")
        @NotBlank
        @Size(min = 6, max = 128)
        String password)
{
}