package ru.vspochernin.errapi.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.errapi.dto.auth.UserDto;
import ru.vspochernin.errapi.exception.ErrorMessage;
import ru.vspochernin.errapi.model.auth.UserRole;
import ru.vspochernin.errapi.security.AuthUserDetails;
import ru.vspochernin.errapi.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
@Tag(name = "Users", description = "Управление пользователями и их ролями")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Получить список пользователей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей получен",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers() {
        List<UserDto> response = userService.listUsers();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(summary = "Получить пользователя по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь найден",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(
            @Parameter(description = "Идентификатор пользователя", example = "1")
            @PathVariable long id)
    {
        UserDto response = userService.getUser(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(summary = "Изменить роль пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль успешно изменена",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный параметр role",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав для смены роли",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> changeRole(
            @Parameter(description = "Идентификатор пользователя", example = "2")
            @PathVariable long id,

            @Parameter(description = "Новая роль пользователя", example = "READER")
            @RequestParam("role") UserRole role,

            @AuthenticationPrincipal AuthUserDetails actor)
    {
        UserDto response = userService.changeRole(id, role, actor);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
