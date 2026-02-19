package ru.vspochernin.errapi.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.vspochernin.errapi.model.User;
import ru.vspochernin.errapi.model.UserRole;
import ru.vspochernin.errapi.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class OwnerInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.OWNER)) {
            return;
        }

        String login = getEnvOrThrow("ERRLOG_OWNER_LOGIN");
        String email = getEnvOrThrow("ERRLOG_OWNER_EMAIL");
        String password = getEnvOrThrow("ERRLOG_OWNER_PASSWORD");

        if (userRepository.existsByLogin(login) || userRepository.existsByEmail(email)) {
            throw new IllegalStateException("OWNER init failed: login/email already exists in database");
        }

        User owner = new User();
        owner.setLogin(login);
        owner.setEmail(email);
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setRole(UserRole.OWNER);

        userRepository.save(owner);
    }

    private static String getEnvOrThrow(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required environment variable " + name);
        }
        return v;
    }
}
