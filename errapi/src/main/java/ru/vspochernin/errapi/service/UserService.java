package ru.vspochernin.errapi.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.vspochernin.errapi.dto.UserDto;
import ru.vspochernin.errapi.model.User;
import ru.vspochernin.errapi.model.UserRole;
import ru.vspochernin.errapi.repository.UserRepository;
import ru.vspochernin.errapi.security.AuthUserDetails;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromUser)
                .toList();
    }

    public UserDto getUser(long id) {
        return UserDto.fromUser(findUserByIdOrThrow(id));
    }

    public UserDto changeRole(long targetUserId, UserRole newRole, AuthUserDetails actor) {
        long actorId = actor.getId();
        if (targetUserId == actorId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        UserRole actorRole = actor.getRole();
        User target = findUserByIdOrThrow(targetUserId);

        if (!actorRole.canModify(target.getRole(), newRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        target.setRole(newRole);
        target = userRepository.save(target);
        return UserDto.fromUser(target);
    }

    private User findUserByIdOrThrow(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
