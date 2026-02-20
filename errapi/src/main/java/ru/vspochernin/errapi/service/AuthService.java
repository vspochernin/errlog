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
import ru.vspochernin.errapi.dto.UserDto;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.User;
import ru.vspochernin.errapi.model.UserRole;
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

        user = userRepository.save(user);
        return UserDto.fromUser(user);
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));

        String token = jwtService.generateToken(request.login());
        return new LoginResponse(token);
    }

    public void changePassword(ChangePasswordRequest request, AuthUserDetails actor) {
        if (!passwordEncoder.matches(request.oldPassword(), actor.getPassword())) {
            throw new ErrapiException(ErrapiErrorType.PASSWORD_DOES_NOT_MATCH);
        }

        if (request.oldPassword().equals(request.newPassword())) {
            throw new ErrapiException(ErrapiErrorType.INVALID_PASSWORD, "Новый пароль совпадает со старым");
        }

        User user = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.BAD_CREDENTIALS));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
