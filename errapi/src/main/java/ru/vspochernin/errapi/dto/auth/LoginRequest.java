package ru.vspochernin.errapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на аутентификацию")
public record LoginRequest(

        @Schema(description = "Логин", example = "owner")
        @NotBlank
        String login,

        @Schema(description = "Пароль", example = "owner_password")
        @NotBlank
        String password)
{
}