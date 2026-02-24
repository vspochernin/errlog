package ru.vspochernin.errapi.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.vspochernin.errapi.dto.auth.UserDto;
import ru.vspochernin.errapi.model.auth.UserRole;
import ru.vspochernin.errapi.security.AuthUserDetails;
import ru.vspochernin.errapi.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> response = userService.listUsers();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable long id) {
        UserDto response = userService.getUser(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> changeRole(
            @PathVariable long id,
            @RequestParam("role") UserRole role,
            @AuthenticationPrincipal AuthUserDetails actor)
    {
        UserDto response = userService.changeRole(id, role, actor);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
