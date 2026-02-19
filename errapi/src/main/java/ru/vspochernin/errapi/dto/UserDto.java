package ru.vspochernin.errapi.dto;

import ru.vspochernin.errapi.model.User;
import ru.vspochernin.errapi.model.UserRole;

public record UserDto(
        Long id,
        String login,
        String email,
        UserRole role)
{

    public static UserDto fromUser(User user) {
        return new UserDto(user.getId(), user.getLogin(), user.getEmail(), user.getRole());
    }
}
