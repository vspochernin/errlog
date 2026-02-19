package ru.vspochernin.errapi.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrorMessage;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ATTR_ERROR_TYPE = "ERRAPI_AUTH_ERROR_TYPE";

    private final SecurityErrorWriter errorWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException
    {
        ErrapiErrorType errorType = (ErrapiErrorType) request.getAttribute(ATTR_ERROR_TYPE);
        if (errorType == null) {
            errorType = ErrapiErrorType.AUTH_REQUIRED;
        }

        errorWriter.write(
                response,
                errorType.getHttpStatus().value(),
                ErrorMessage.fromErrorType(errorType));
    }
}
