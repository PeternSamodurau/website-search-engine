package searchengine.dto.error;

import lombok.Data;

@Data
public class ErrorResponseDTO {
    private boolean result = false;
    private String error;
}