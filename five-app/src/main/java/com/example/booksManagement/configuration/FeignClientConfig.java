package com.example.booksManagement.configuration;

import feign.Response;
import feign.codec.ErrorDecoder;
import feign.FeignException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration class provides a custom ErrorDecoder for our Feign clients.
 * Its main purpose is to gracefully handle server-side errors (5xx) from the OpenLibrary API,
 * which incorrectly returns an HTML page instead of a JSON error object.
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * CustomErrorDecoder intercepts failed API responses.
     * It prevents Feign from trying to parse an HTML error page as JSON,
     * which would otherwise cause a "no suitable HttpMessageConverter" error.
     */
    public static class CustomErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultErrorDecoder = new Default();

        /**
         * This method is called by Feign whenever an API call returns a non-2xx status.
         * @param methodKey A description of the method that was called (e.g., "OpenLibraryClient#searchBooksByCategory(String,int)").
         * @param response The full HTTP response from the server.
         * @return An Exception that will be thrown by the Feign client and caught by our try-catch block.
         */
        @Override
        public Exception decode(String methodKey, Response response) {
            // The OpenLibrary API often returns a 5xx status with an HTML body on failure.
            // We check for this case specifically to prevent the default decoder from reading the body.
            if (response.status() >= 500 && response.status() <= 599) {
                // By calling FeignException.errorStatus, we create a generic FeignException
                // based *only* on the status and headers, WITHOUT attempting to read the response body.
                // This is the key to avoiding the HttpMessageConverter error.
                return FeignException.errorStatus(methodKey, response);
            }

            // For any other type of error (e.g., 404 Not Found), we let the default
            // decoder handle it, as it might contain a readable JSON error message.
            return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
