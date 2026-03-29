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
                                - регистрация и аутентификация пользователя, изменение пароля,
                                - получение информации о пользоватлях и управление их ролями,
                                - получение списка доступных полей и операций для фильтрации,
                                - получение списка групп событий ошибок с возможностью фильтрации,
                                - получение списка событий ошибок с возможностью фильтрации,
                                - получение информации о конкретном событии ошибки,
                                - получение временного ряда количества событий ошибок с возможностью фильтрации.
                                
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
