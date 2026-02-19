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
    INVALID_PASSWORD(4, "Некорректный пароль", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(5, "Некорректный email", HttpStatus.BAD_REQUEST),
    BAD_CREDENTIALS(6, "Пользователь с таким логином или паролем не найден", HttpStatus.UNAUTHORIZED),
    BAD_REQUEST_BODY(7, "Некорректное тело запроса", HttpStatus.BAD_REQUEST),
    NOT_FOUND(8, "Запрашиваемый элемент не найден", HttpStatus.NOT_FOUND),
    PASSWORD_DOES_NOT_MATCH(9, "Введенный пароль не совпадает со старым", HttpStatus.UNAUTHORIZED),
    INCORRECT_ROLE_CHANGE(10, "Некорректное изменение роли", HttpStatus.FORBIDDEN),
    AUTH_REQUIRED(11, "Требуется аутентификация", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(12, "Недостаточно прав", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(13, "Некорректный токен", HttpStatus.UNAUTHORIZED),
    BAD_REQUEST_QUERY(14, "Некорректные параметры запроса", HttpStatus.BAD_REQUEST),
    ;

    private final int id;
    private final String description;
    private final HttpStatus httpStatus;
}
