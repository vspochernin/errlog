package ru.vspochernin.errapi.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.vspochernin.errapi.model.auth.User;
import ru.vspochernin.errapi.model.auth.UserRole;
import ru.vspochernin.errapi.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class OwnerInitializer implements ApplicationRunner {

    private static final String OWNER_LOGIN_ENV = "ERRLOG_OWNER_LOGIN";
    private static final String OWNER_EMAIL_ENV = "ERRLOG_OWNER_EMAIL";
    private static final String OWNER_PASSWORD_ENV = "ERRLOG_OWNER_PASSWORD";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.OWNER)) {
            return; // Создаем владельца только если его еще нет.
        }

        String login = getEnvOrThrow(OWNER_LOGIN_ENV);
        String email = getEnvOrThrow(OWNER_EMAIL_ENV);
        String password = getEnvOrThrow(OWNER_PASSWORD_ENV);

        if (userRepository.existsByLogin(login) || userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Owner init failed: login/email already exists in database");
        }

        User owner = new User();
        owner.setLogin(login);
        owner.setEmail(email);
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setRole(UserRole.OWNER);

        userRepository.save(owner);
    }

    private static String getEnvOrThrow(String name) {
        String env = System.getenv(name);
        if (env == null || env.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return env;
    }
}
