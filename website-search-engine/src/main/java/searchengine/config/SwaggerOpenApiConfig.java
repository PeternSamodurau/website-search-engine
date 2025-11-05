package searchengine.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Поисковый движок для сайтов",
                description = "API для поискового движка. Позволяет управлять индексацией сайтов и выполнять поиск по проиндексированным данным.",
                version = "1.0.0",
                contact = @Contact(
                        name = "Piter Samodurov",
                        email = "spvrent@mail.ru"
                )
        )
)
public class SwaggerOpenApiConfig {
}