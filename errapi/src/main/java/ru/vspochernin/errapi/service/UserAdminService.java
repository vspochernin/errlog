package ru.vspochernin.errapi.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.vspochernin.errapi.dto.auth.UserDto;
import ru.vspochernin.errapi.model.User;
import ru.vspochernin.errapi.model.UserRole;
import ru.vspochernin.errapi.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;

    public List<UserDto> listUsers() {
        return userRepository.findAll().stream()
                .map(UserDto::fromUser)
                .toList();
    }

    public UserDto getUser(long id) {
        return UserDto.fromUser(findUserByIdOrThrow(id));
    }

    public UserDto changeRole(long targetUserId, UserRole newRole, UserRole actorRole) {
        User target = findUserByIdOrThrow(targetUserId);

        // OWNER'ов никто не может менять.
        if (target.getRole() == UserRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OWNER role can't be changed");
        }

        // ADMIN может назначать только READER и NONE.
        if (actorRole == UserRole.ADMIN) {
            if (!(newRole == UserRole.READER || newRole == UserRole.NONE)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN can assign only READER or NONE");
            }
        }

        // READER и NONE не может назначать никого.
        if (actorRole == UserRole.READER || actorRole == UserRole.NONE) {
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
