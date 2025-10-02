package com.example.booksManagement.configuration;

import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    public static class CustomErrorDecoder implements ErrorDecoder {
        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            Exception exception = defaultErrorDecoder.decode(methodKey, response);

            // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
            // Используем полное имя класса, чтобы компилятор его точно нашел
            return new com.example.booksManagement.exception.CustomFeignException(
                    response.status(),
                    exception.getMessage(),
                    response.request().headers()
            );
        }
    }
}