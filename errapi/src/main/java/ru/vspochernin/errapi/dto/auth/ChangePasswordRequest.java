package ru.vspochernin.errapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на смену пароля пользователя")
public record ChangePasswordRequest(

        @Schema(description = "Текущий пароль", example = "old_password")
        @NotBlank
        String oldPassword,

        @Schema(description = "Новый пароль", example = "new_password")
        @NotBlank
        @Size(min = 6, max = 128)
        String newPassword)
{
}
