package com.example.seven_app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Basic Authentication for API access")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }

    @Bean
    public OpenApiCustomizer customize(SwaggerCache swaggerCache) {
        return openApi -> {
            final List<String> userIds = swaggerCache.getUserIds();
            final List<String> taskIds = swaggerCache.getTaskIds();

            openApi.getPaths().values().forEach(pathItem -> {
                pathItem.readOperations().forEach(operation -> {

                    List<String> tags = operation.getTags();
                    boolean isTaskOperation = tags != null && tags.stream().anyMatch(tag -> tag.contains("Task"));

                    if (operation.getParameters() == null) {
                        return;
                    }

                    for (Parameter parameter : operation.getParameters()) {

                        if (parameter.getName().equals("id")) {
                            if (isTaskOperation && !taskIds.isEmpty()) {

                                Schema<String> schema = new Schema<>();
                                schema.setType("string");
                                schema.setEnum(taskIds);
                                parameter.setSchema(schema);
                            } else if (!isTaskOperation && !userIds.isEmpty()) {

                                Schema<String> schema = new Schema<>();
                                schema.setType("string");
                                schema.setEnum(userIds);
                                parameter.setSchema(schema);
                            }
                        }


                        if (!userIds.isEmpty() && (
                                parameter.getName().equals("assigneeId") ||
                                        parameter.getName().equals("observerId")
                        )) {
                            Schema<String> schema = new Schema<>();
                            schema.setType("string");
                            schema.setEnum(userIds);
                            parameter.setSchema(schema);
                        }
                    }
                });
            });
        };
    }
}
