package ru.vspochernin.errapi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
public class OpenApiConfig {

    @Bean
    public OpenAPI errlogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Errlog API")
                        .version("1.0")
                        .description("""
                                REST API сервиса сбора и анализа ошибок информационных систем Errlog.
                                
                                Возможности текущей версии API:
                                - Регистрация и аутентификация пользователей.
                                - Управление ролями пользователей.
                                - Получение списка возможных для фильтрации полей и операций.
                                - Получение списка групп событий (с возможностью фильтрации).
                                - Получение списка событий (с возможностью фильтрации).
                                - Получение подробной информации о конкретном событии.
                                - Получение временного ряда количества событий ошибок (c возможностью фильтрации).
                                
                                Для защищенных эндпоинтов используется JWT Bearer token.
                                """));
    }

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
