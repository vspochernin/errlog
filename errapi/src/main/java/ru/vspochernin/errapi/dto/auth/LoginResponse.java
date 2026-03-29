package ru.vspochernin.errapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ запроса на аутентификацию")
public record LoginResponse(

        @Schema(
                description = "JWT токен для Authorization: Bearer <token>",
                example = "token")
        String token)
{
}
