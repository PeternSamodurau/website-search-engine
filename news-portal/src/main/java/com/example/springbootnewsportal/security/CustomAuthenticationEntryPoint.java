package com.example.springbootnewsportal.security;

import com.example.springbootnewsportal.dto.response.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        
        String errorMessage = "Неверный логин или пароль.";

        log.error("Ошибка аутентификации: {}", errorMessage);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                HttpServletResponse.SC_UNAUTHORIZED,
                errorMessage
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
