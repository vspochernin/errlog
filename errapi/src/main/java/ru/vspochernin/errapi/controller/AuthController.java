package ru.vspochernin.errapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vspochernin.errapi.dto.auth.ChangePasswordRequest;
import ru.vspochernin.errapi.dto.auth.LoginRequest;
import ru.vspochernin.errapi.dto.auth.LoginResponse;
import ru.vspochernin.errapi.dto.auth.RegisterRequest;
import ru.vspochernin.errapi.dto.auth.UserDto;
import ru.vspochernin.errapi.exception.ErrorMessage;
import ru.vspochernin.errapi.security.AuthUserDetails;
import ru.vspochernin.errapi.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Регистрация, аутентификация и смена пароля пользователя")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя с ролью NONE")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Успешная регистрация нового пользователя",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Учетные данные нового пользователя",
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "login": "new_user",
                                              "email": "newuser@example.com",
                                              "password": "new_user_password"
                                            }
                                            """)))
            @RequestBody RegisterRequest request)
    {
        UserDto response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Аутентификация",
            description = "Возвращает JWT токен для дальнейшей работы с защищенными эндпоинтами")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Учетные данные пользователя",
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "login": "owner",
                                              "password": "owner_password"
                                            }
                                            """)))
            @RequestBody LoginRequest request)
    {
        LoginResponse response = authService.login(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Operation(
            summary = "Смена пароля",
            description = "Требует валидный JWT токен аутентифицированного пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Успешное изменение пароля"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "401", description = "Ошибка аутентификации или несоответствие паролей",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Старый и новый пароли",
                    content = @Content(schema = @Schema(implementation = ChangePasswordRequest.class)))
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthUserDetails actor)
    {
        authService.changePassword(request, actor);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
