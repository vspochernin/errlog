package ru.vspochernin.errapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrapiErrorType {

    UNEXPECTED_ERROR(0, "Непредвиденная ошибка", HttpStatus.INTERNAL_SERVER_ERROR),

    LOGIN_EXISTS(1, "Пользователь с таким логином уже существует", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTS(2, "Пользователь с таким email уже существует", HttpStatus.BAD_REQUEST),

    INVALID_LOGIN(3, "Некорректный логин", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(4, "Некорректный email", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(5, "Некорректный пароль", HttpStatus.BAD_REQUEST),

    PASSWORD_DOES_NOT_MATCH(6, "Введенный пароль не совпадает со старым", HttpStatus.UNAUTHORIZED),
    INCORRECT_ROLE_CHANGE(7, "Некорректное изменение роли", HttpStatus.FORBIDDEN),

    BAD_REQUEST(8, "Некорректный запрос", HttpStatus.BAD_REQUEST),
    NOT_FOUND(9, "Запрашиваемый элемент не найден", HttpStatus.NOT_FOUND),

    BAD_CREDENTIALS(10, "Пользователь с таким логином или паролем не найден", HttpStatus.UNAUTHORIZED),
    USER_DOES_NOT_EXIST(11, "Пользователь не существует", HttpStatus.UNAUTHORIZED),
    AUTH_REQUIRED(12, "Требуется аутентификация", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(13, "Некорректный токен", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(14, "Недостаточно прав", HttpStatus.FORBIDDEN),

    INCORRECT_TIME_BORDERS(15, "Некорректные границы времени", HttpStatus.BAD_REQUEST),
    ;

    private final int id;
    private final String description;
    private final HttpStatus httpStatus;
}
