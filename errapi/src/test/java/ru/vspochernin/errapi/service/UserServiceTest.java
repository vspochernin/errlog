package ru.vspochernin.errapi.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.auth.User;
import ru.vspochernin.errapi.model.auth.UserRole;
import ru.vspochernin.errapi.repository.UserRepository;
import ru.vspochernin.errapi.security.AuthUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void listUsersShouldReturnAll() {
        var user = new User();
        user.setId(1);
        user.setLogin("test");
        user.setEmail("test@example.com");
        user.setRole(UserRole.READER);
        when(userRepository.findAll()).thenReturn(List.of(user));

        var result = userService.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().login()).isEqualTo("test");
    }

    @Test
    void getUserShouldReturnUserWhenFound() {
        var user = new User();
        user.setId(1);
        user.setLogin("test");
        user.setRole(UserRole.READER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var result = userService.getUser(1);

        assertThat(result.login()).isEqualTo("test");
    }

    @Test
    void getUserShouldThrowWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(999))
                .isInstanceOf(ErrapiException.class);
    }

    @Test
    void changeRoleShouldSucceedForValidChange() {
        var owner = new AuthUserDetails(createTestUser(1, "owner", UserRole.OWNER));
        var target = new User();
        target.setId(2);
        target.setLogin("reader");
        target.setRole(UserRole.READER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        var result = userService.changeRole(2, UserRole.NONE, owner);

        assertThat(result.role()).isEqualTo(UserRole.NONE);
    }

    @Test
    void changeRoleShouldThrowWhenChangingOwnRole() {
        var admin = new AuthUserDetails(createTestUser(1, "admin", UserRole.ADMIN));

        assertThatThrownBy(() -> userService.changeRole(1, UserRole.READER, admin))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Can't change own role");
    }

    private static User createTestUser(long id, String login, UserRole role) {
        var user = new User();
        user.setId(id);
        user.setLogin(login);
        user.setEmail(login + "@test.com");
        user.setPasswordHash("hash");
        user.setRole(role);
        return user;
    }
}
