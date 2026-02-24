package ru.vspochernin.errapi.dto.auth;

import ru.vspochernin.errapi.model.auth.User;
import ru.vspochernin.errapi.model.auth.UserRole;

public record UserDto(
        long id,
        String login,
        String email,
        UserRole role)
{

    public static UserDto fromUser(User user) {
        return new UserDto(user.getId(), user.getLogin(), user.getEmail(), user.getRole());
    }
}
