package ru.vspochernin.errapi.dto.auth;

import ru.vspochernin.errapi.model.UserRole;

public record UserDto(
        Long id,
        String login,
        String email,
        UserRole role)
{
}
