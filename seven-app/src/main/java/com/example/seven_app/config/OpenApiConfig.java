package com.example.seven_app.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OperationCustomizer userDropdownCustomizer(SwaggerUserCache userCache, SwaggerTaskCache taskCache) {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            List<String> userIds = userCache.getUserIds();
            List<String> taskIds = taskCache.getTaskIds();

            if (operation.getParameters() != null) {
                for (Parameter parameter : operation.getParameters()) {
                    // Dropdown for User IDs
                    if (userIds != null && !userIds.isEmpty()) {
                        if ("assigneeId".equals(parameter.getName()) || "observerId".equals(parameter.getName())) {
                            StringSchema newSchema = new StringSchema();
                            newSchema.setEnum(userIds);
                            parameter.setSchema(newSchema);
                        }
                    }

                    // Dropdown for Task IDs
                    if (taskIds != null && !taskIds.isEmpty()) {
                        if ("taskId".equals(parameter.getName())) {
                            StringSchema newSchema = new StringSchema();
                            newSchema.setEnum(taskIds);
                            parameter.setSchema(newSchema);
                        }
                    }
                }
            }
            return operation;
        };
    }
}
