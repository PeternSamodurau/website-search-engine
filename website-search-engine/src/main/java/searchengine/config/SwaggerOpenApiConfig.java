package searchengine.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Books Management API",
                version = "v1",
                description = "API для управления книгами и категориями",
                contact = @Contact(
                        name = "Peter_Samodurov",
                        email = "spvrent@mail.ru"
                )
        )
)
public class SwaggerOpenApiConfig {
}
