package ru.vspochernin.errapi.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.vspochernin.errapi.model.auth.User;
import ru.vspochernin.errapi.model.auth.UserRole;

@Schema(description = "Пользователь сервиса")
public record UserDto(

        @Schema(description = "Идентификатор пользователя", example = "1")
        long id,

        @Schema(description = "Логин пользователя", example = "owner")
        String login,

        @Schema(description = "Email пользователя", example = "owner@example.com")
        String email,

        @Schema(description = "Роль пользователя", example = "OWNER")
        UserRole role)
{

    public static UserDto fromUser(User user) {
        return new UserDto(user.getId(), user.getLogin(), user.getEmail(), user.getRole());
    }
}
