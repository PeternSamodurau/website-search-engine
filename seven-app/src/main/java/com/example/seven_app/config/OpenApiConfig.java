package com.example.seven_app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer customize(SwaggerCache swaggerCache) {
        return openApi -> {
            final List<String> userIds = swaggerCache.getUserIds();
            final List<String> taskIds = swaggerCache.getTaskIds();

            openApi.getPaths().values().forEach(pathItem -> {
                pathItem.readOperations().forEach(operation -> {
                    // Получаем тэги операции, чтобы понять ее контекст (Task или User)
                    List<String> tags = operation.getTags();
                    boolean isTaskOperation = tags != null && tags.stream().anyMatch(tag -> tag.contains("Task"));

                    if (operation.getParameters() == null) {
                        return;
                    }

                    for (Parameter parameter : operation.getParameters()) {
                        // --- НОВАЯ, УМНАЯ ЛОГИКА ---
                        if (parameter.getName().equals("id")) {
                            if (isTaskOperation && !taskIds.isEmpty()) {
                                // Если это операция с задачей, применяем taskIds
                                Schema<String> schema = new Schema<>();
                                schema.setType("string");
                                schema.setEnum(taskIds);
                                parameter.setSchema(schema);
                            } else if (!isTaskOperation && !userIds.isEmpty()) {
                                // Если это операция с пользователем, применяем userIds
                                Schema<String> schema = new Schema<>();
                                schema.setType("string");
                                schema.setEnum(userIds);
                                parameter.setSchema(schema);
                            }
                        }

                        // --- СТАРАЯ ЛОГИКА ДЛЯ assigneeId и observerId (остается без изменений) ---
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