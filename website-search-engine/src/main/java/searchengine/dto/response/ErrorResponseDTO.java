package searchengine.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDTO {
    private boolean result = false;
    private String error;

    public ErrorResponseDTO(String errorMessage) {
        this.error = errorMessage;
    }
}
