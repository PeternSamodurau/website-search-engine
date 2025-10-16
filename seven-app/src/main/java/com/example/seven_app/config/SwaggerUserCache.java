package com.example.seven_app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Getter
@Setter
public class SwaggerUserCache {

    private List<String> userIds = Collections.emptyList();

}
