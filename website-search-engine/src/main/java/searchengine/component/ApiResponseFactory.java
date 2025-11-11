package searchengine.component;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiResponseFactory {

    /**
     * Создает стандартный успешный ответ: { "result": true }
     * @return ResponseEntity
     */
    public ResponseEntity<Map<String, Object>> createSuccessResponse() {
        return ResponseEntity.ok(Collections.singletonMap("result", true));
    }

    /**
     * Создает стандартный ответ с ошибкой: { "result": false, "error": "..." }
     * @param errorMessage Сообщение об ошибке
     * @return ResponseEntity
     */
    public ResponseEntity<Map<String, Object>> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", false);
        response.put("error", errorMessage);
        return ResponseEntity.badRequest().body(response);
    }
}
