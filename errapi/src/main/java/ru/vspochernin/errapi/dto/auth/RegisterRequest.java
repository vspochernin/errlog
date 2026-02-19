package ru.vspochernin.errapi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String login,
        @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 6, max = 128) String password)
{
}
