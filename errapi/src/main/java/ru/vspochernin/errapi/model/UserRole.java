package ru.vspochernin.errapi.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserRole {

    OWNER(1000),
    ADMIN(100),
    READER(10),
    NONE(0);

    private final int level;

    public boolean canModify(UserRole targetRole, UserRole newRole) {
        // Нельзя что-либо делать с владельцем.
        if (targetRole == OWNER) {
            return false;
        }

        // Нельзя назначать владельца (назначается один раз при запуске приложения).
        if (newRole == OWNER) {
            return false;
        }

        // Нельзя трогать роли высшие или равные своему уровню.
        if (targetRole.level >= this.level) {
            return false;
        }

        // Нельзя назначать роли высшие или равные своему уровню.
        if (newRole.level >= this.level) {
            return false;
        }

        return true;
    }
}
