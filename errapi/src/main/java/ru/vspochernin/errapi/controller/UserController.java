package ru.vspochernin.errapi.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vspochernin.errapi.dto.UserDto;
import ru.vspochernin.errapi.model.UserRole;
import ru.vspochernin.errapi.security.AuthUserDetails;
import ru.vspochernin.errapi.service.UserService;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    List<UserDto> listUsers() {
        return userService.listUsers();
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable long id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}/role")
    public UserDto changeRole(
            @PathVariable long id,
            @RequestParam("role") UserRole role,
            @AuthenticationPrincipal AuthUserDetails actor)
    {
        return userService.changeRole(id, role, actor);
    }
}
