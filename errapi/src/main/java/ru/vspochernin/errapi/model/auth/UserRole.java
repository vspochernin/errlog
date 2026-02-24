package ru.vspochernin.errapi.model.auth;

import lombok.RequiredArgsConstructor;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;

@RequiredArgsConstructor
public enum UserRole {

    OWNER(3),
    ADMIN(2),
    READER(1),
    NONE(0);

    private final int level; // Абсолютный "уровень" роли для сравнения ее "силы" с остальными ролями.

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
