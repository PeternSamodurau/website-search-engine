package searchengine.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponseDTO {
    private boolean result;
    private String error;
    private int count;
    private List<SearchDataDTO> data;

    public SearchResponseDTO(boolean result) {
        this.result = result;
    }

    public SearchResponseDTO(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public SearchResponseDTO(boolean result, int count, List<SearchDataDTO> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }
}
