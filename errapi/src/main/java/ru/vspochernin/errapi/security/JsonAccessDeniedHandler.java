package ru.vspochernin.errapi.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrorMessage;

// Кастомный AccessDeniedHandler для формирования json ответа, когда аутентифицированному пользователю не хватает прав.
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errorWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException
    {
        ErrapiErrorType errorType = ErrapiErrorType.FORBIDDEN;
        errorWriter.write(
                response,
                errorType.getHttpStatus().value(),
                ErrorMessage.fromErrorType(errorType));
    }
}
