package com.example.seven_app.config;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    // Используем новый единый SwaggerCache
    public OperationCustomizer customize(SwaggerCache swaggerCache) {
        return (operation, handlerMethod) -> {
            // Получаем оба списка из единого кэша
            List<String> userIds = swaggerCache.getUserIds();
            List<String> taskIds = swaggerCache.getTaskIds();

            if (operation.getParameters() == null) {
                return operation;
            }

            // ЕДИНЫЙ ЦИКЛ ДЛЯ ВСЕХ ПАРАМЕТРОВ (логика без изменений)
            for (Parameter parameter : operation.getParameters()) {

                // Логика для ID пользователей
                if (!userIds.isEmpty() && (
                        parameter.getName().equals("assigneeId") ||
                                parameter.getName().equals("observerId") ||
                                parameter.getName().equals("userId")
                )) {
                    Schema<String> schema = new Schema<>();
                    schema.setType("string");
                    schema.setEnum(userIds);
                    parameter.setSchema(schema);
                }

                // Логика для ID задач
                if (!taskIds.isEmpty() && parameter.getName().equals("taskId")) {
                    Schema<String> schema = new Schema<>();
                    schema.setType("string");
                    schema.setEnum(taskIds);
                    parameter.setSchema(schema);
                }
            }
            return operation;
        };
    }
}