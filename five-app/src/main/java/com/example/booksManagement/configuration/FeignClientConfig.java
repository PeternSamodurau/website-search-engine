package com.example.booksManagement.configuration;

import feign.Response;
import feign.codec.ErrorDecoder;
import feign.FeignException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {

        return new CustomErrorDecoder();
    }

    public static class CustomErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {

            if (response.status() >= 500 && response.status() <= 599) {

                return FeignException.errorStatus(methodKey, response);
            }

            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
