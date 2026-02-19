package ru.vspochernin.errapi.model;

import lombok.RequiredArgsConstructor;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;

@RequiredArgsConstructor
public enum UserRole {

    OWNER(1000),
    ADMIN(100),
    READER(10),
    NONE(0);

    private final int level;

    public void validateCanModify(UserRole targetRole, UserRole newRole) {
        // Нельзя менять роль владельца.
        if (targetRole == OWNER) {
            throw new ErrapiException(ErrapiErrorType.INCORRECT_ROLE_CHANGE, "Нельзя менять роль владельца");
        }

        // Нельзя назначать владельца (назначается один раз при запуске приложения).
        if (newRole == OWNER) {
            throw new ErrapiException(ErrapiErrorType.INCORRECT_ROLE_CHANGE, "Нельзя назначать владельца");
        }

        // Нельзя менять роли высшие или равные своему уровню.
        if (targetRole.level >= this.level) {
            throw new ErrapiException(ErrapiErrorType.INCORRECT_ROLE_CHANGE, "Нельзя менять высшую или равную роль");
        }

        // Нельзя назначать роли высшие или равные своему уровню.
        if (newRole.level >= this.level) {
            throw new ErrapiException(ErrapiErrorType.INCORRECT_ROLE_CHANGE, "Нельзя назначать высшую или равную роль");
        }
    }
}
