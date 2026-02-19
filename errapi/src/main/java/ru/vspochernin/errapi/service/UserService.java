package ru.vspochernin.errapi.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vspochernin.errapi.dto.UserDto;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
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
            throw new ErrapiException(ErrapiErrorType.INCORRECT_ROLE_CHANGE, "Нельзя менять свою роль");
        }

        UserRole actorRole = actor.getRole();
        User target = findUserByIdOrThrow(targetUserId);

        UserRole targetRole = target.getRole();
        actorRole.validateCanModify(targetRole, newRole);

        target.setRole(newRole);
        target = userRepository.save(target);
        return UserDto.fromUser(target);
    }

    private User findUserByIdOrThrow(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ErrapiException(ErrapiErrorType.NOT_FOUND));
    }
}
