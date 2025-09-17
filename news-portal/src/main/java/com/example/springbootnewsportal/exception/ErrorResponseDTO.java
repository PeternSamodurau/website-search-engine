package com.example.springbootnewsportal.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseDTO {
    private int status;
    private String message;
}
