package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.error.ErrorResponseDTO;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Новый, специфичный обработчик для ошибок индексации
    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<ErrorResponseDTO> handleIndexingException(IndexingException exception) {
        log.warn("Ошибка бизнес-логики: {}", exception.getMessage()); // Логируем как предупреждение
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setError(exception.getMessage()); // Отдаем клиенту точное сообщение
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // Возвращаем статус 400
    }

    // "Запасной" обработчик для всех остальных непредвиденных ошибок
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAllUncaughtException(Exception exception) {
        log.error("Произошла непредвиденная ошибка", exception); // Логируем как критическую ошибку
        
        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setError("На сервере произошла непредвиденная ошибка.");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // Возвращаем статус 500
    }
}
