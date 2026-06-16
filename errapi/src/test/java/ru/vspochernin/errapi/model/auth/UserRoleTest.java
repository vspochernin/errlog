package ru.vspochernin.errapi.model.auth;

import org.junit.jupiter.api.Test;
import ru.vspochernin.errapi.exception.ErrapiException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRoleTest {

    @Test
    void ownerCanChangeAdmin() {
        assertThatCode(() -> UserRole.OWNER.validateCanModify(UserRole.ADMIN, UserRole.READER))
                .doesNotThrowAnyException();
    }

    @Test
    void ownerCanChangeReader() {
        assertThatCode(() -> UserRole.OWNER.validateCanModify(UserRole.READER, UserRole.NONE))
                .doesNotThrowAnyException();
    }

    @Test
    void ownerCanChangeNone() {
        assertThatCode(() -> UserRole.OWNER.validateCanModify(UserRole.NONE, UserRole.READER))
                .doesNotThrowAnyException();
    }

    @Test
    void ownerCannotChangeOwner() {
        assertThatThrownBy(() -> UserRole.OWNER.validateCanModify(UserRole.OWNER, UserRole.ADMIN))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Can't change owner role");
    }

    @Test
    void cannotAssignOwner() {
        assertThatThrownBy(() -> UserRole.OWNER.validateCanModify(UserRole.ADMIN, UserRole.OWNER))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Can't assign owner role");
    }

    @Test
    void adminCannotChangeAdmin() {
        assertThatThrownBy(() -> UserRole.ADMIN.validateCanModify(UserRole.ADMIN, UserRole.READER))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("higher or equal role");
    }

    @Test
    void adminCannotAssignAdmin() {
        assertThatThrownBy(() -> UserRole.ADMIN.validateCanModify(UserRole.READER, UserRole.ADMIN))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("higher or equal role");
    }

    @Test
    void adminCanChangeReader() {
        assertThatCode(() -> UserRole.ADMIN.validateCanModify(UserRole.READER, UserRole.NONE))
                .doesNotThrowAnyException();
    }

    @Test
    void readerCannotChangeReader() {
        assertThatThrownBy(() -> UserRole.READER.validateCanModify(UserRole.READER, UserRole.NONE))
                .isInstanceOf(ErrapiException.class);
    }

    @Test
    void readerCanChangeNone() {
        // READER (уровень 1) может изменить NONE (уровень 0) — targetRole.level (0) < reader.level (1), ок
        assertThatCode(() -> UserRole.READER.validateCanModify(UserRole.NONE, UserRole.NONE))
                .doesNotThrowAnyException();
    }
}
