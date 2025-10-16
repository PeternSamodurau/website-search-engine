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
    public OperationCustomizer userDropdownCustomizer(SwaggerUserCache userCache) {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            List<String> userIds = userCache.getUserIds();

            if (userIds == null || userIds.isEmpty()) {
                return operation;
            }

            if (operation.getParameters() != null) {
                for (Parameter parameter : operation.getParameters()) {
                    if ("assigneeId".equals(parameter.getName()) || "observerId".equals(parameter.getName())) {
                        StringSchema newSchema = new StringSchema();
                        newSchema.setEnum(userIds);
                        parameter.setSchema(newSchema);
                    }
                }
            }
            return operation;
        };
    }
}
