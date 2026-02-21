package ru.vspochernin.errapi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// Объявляем в OpenAPI-спецификации схему безопасности с именем bearerAuth.
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig { // Для появления в Swagger UI кнопки "Authorize" и удобной аутентификации в Swagger.

    // Чтобы Swagger начал прикреплять токен к запросам.
    @Bean
    public OpenApiCustomizer securityPerOperationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> {
                // Чтобы публичные эндпоинты были без аутентификации.
                if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
                    return;
                }

                item.readOperations().forEach(op ->
                        op.addSecurityItem(new SecurityRequirement().addList("bearerAuth")));
            });
        };
    }
}
