package com.example.seven_app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Task Tracker with Spring Security",
                version = "v1",
                description = "API для управления пользователями, авторами и их задачами",
                contact = @Contact(
                        name = "Peter_Samodurov",
                        email = "spvrent@mail.ru"
                )
        )
)
public class SwaggerOpenApiConfig {
}
