package ru.vspochernin.errapi.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.vspochernin.errapi.dto.auth.ChangePasswordRequest;
import ru.vspochernin.errapi.dto.auth.LoginRequest;
import ru.vspochernin.errapi.dto.auth.RegisterRequest;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.auth.User;
import ru.vspochernin.errapi.model.auth.UserRole;
import ru.vspochernin.errapi.repository.UserRepository;
import ru.vspochernin.errapi.security.AuthUserDetails;
import ru.vspochernin.errapi.security.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registerShouldCreateUserWithRoleNone() {
        when(userRepository.existsByLogin("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        var saved = new User();
        saved.setId(2);
        saved.setLogin("newuser");
        saved.setEmail("new@test.com");
        saved.setRole(UserRole.NONE);
        when(userRepository.save(any())).thenReturn(saved);

        var result = authService.register(new RegisterRequest("newuser", "new@test.com", "secret"));

        assertThat(result.role()).isEqualTo(UserRole.NONE);
        assertThat(result.login()).isEqualTo("newuser");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.NONE);
    }

    @Test
    void registerShouldThrowWhenLoginExists() {
        when(userRepository.existsByLogin("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("existing", "e@t.com", "secret")))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Login already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldThrowWhenEmailExists() {
        when(userRepository.existsByLogin("new")).thenReturn(false);
        when(userRepository.existsByEmail("existing@t.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("new", "existing@t.com", "secret")))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void loginShouldReturnTokenOnSuccess() {
        var req = new LoginRequest("owner", "pass");
        when(jwtService.generateToken("owner")).thenReturn("some.jwt.token");

        var result = authService.login(req);

        assertThat(result.token()).isEqualTo("some.jwt.token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("owner", "pass"));
    }

    @Test
    void loginShouldThrowOnBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("owner", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void changePasswordShouldUpdateHash() {
        var actor = new AuthUserDetails(createTestUser(1, "user", UserRole.READER));
        when(passwordEncoder.matches("old", actor.getPassword())).thenReturn(true);
        var dbUser = new User();
        dbUser.setId(1);
        dbUser.setPasswordHash("oldhash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(dbUser));
        when(passwordEncoder.encode("newpass")).thenReturn("newhash");

        authService.changePassword(new ChangePasswordRequest("old", "newpass"), actor);

        verify(userRepository).save(dbUser);
        assertThat(dbUser.getPasswordHash()).isEqualTo("newhash");
    }

    @Test
    void changePasswordShouldThrowWhenOldPasswordDoesNotMatch() {
        var actor = new AuthUserDetails(createTestUser(1, "user", UserRole.READER));
        when(passwordEncoder.matches("wrong", actor.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(
                new ChangePasswordRequest("wrong", "newpass"), actor))
                .isInstanceOf(ErrapiException.class)
                .hasMessageContaining("Old password does not match");
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
