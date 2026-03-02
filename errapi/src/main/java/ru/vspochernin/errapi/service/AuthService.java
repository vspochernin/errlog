package ru.vspochernin.errapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.vspochernin.errapi.dto.auth.ChangePasswordRequest;
import ru.vspochernin.errapi.dto.auth.LoginRequest;
import ru.vspochernin.errapi.dto.auth.RegisterRequest;
import ru.vspochernin.errapi.dto.auth.LoginResponse;
import ru.vspochernin.errapi.dto.auth.UserDto;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.auth.User;
import ru.vspochernin.errapi.model.auth.UserRole;
import ru.vspochernin.errapi.repository.UserRepository;
import ru.vspochernin.errapi.security.AuthUserDetails;
import ru.vspochernin.errapi.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByLogin(request.login())) {
            throw new ErrapiException(ErrapiErrorType.LOGIN_EXISTS);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ErrapiException(ErrapiErrorType.EMAIL_EXISTS);
        }

        User user = new User();
        user.setLogin(request.login());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.NONE);

        return UserDto.fromUser(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));

        // Если authenticate() провалится - до сюда не дойдем.
        return new LoginResponse(jwtService.generateToken(request.login()));
    }

    public void changePassword(ChangePasswordRequest request, AuthUserDetails actor) {
        if (!passwordEncoder.matches(request.oldPassword(), actor.getPassword())) {
            throw new ErrapiException(ErrapiErrorType.PASSWORD_DOES_NOT_MATCH);
        }
        if (request.oldPassword().equals(request.newPassword())) {
            throw new ErrapiException(ErrapiErrorType.INVALID_PASSWORD, "New password matches the old one");
        }

        User user = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.BAD_CREDENTIALS));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword())); // Обновляем пароль.

        userRepository.save(user);
    }
}
