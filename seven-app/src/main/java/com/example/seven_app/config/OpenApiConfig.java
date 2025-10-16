package com.example.seven_app.config;

import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer userDropdownCustomiser(SwaggerUserCache userCache) {
        return openApi -> {
            List<String> userIds = userCache.getUserIds();

            if (userIds.isEmpty()) {
                return; // No users to add to the dropdown
            }

            // Iterate through all paths and operations to find the relevant parameters
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                if (operation.getParameters() != null) {
                    operation.getParameters().forEach(parameter -> {
                        // Check for parameters used for assigning users
                        if ("assigneeId".equals(parameter.getName()) || "observerId".equals(parameter.getName())) {
                            if (parameter.getSchema() != null) {
                                // Set the list of user IDs as an enum, which Swagger UI renders as a dropdown
                                parameter.getSchema().setEnum(userIds);
                            }
                        }
                    });
                }
            }));
        };
    }
}
