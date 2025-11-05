package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.error.ErrorResponseDTO;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<ErrorResponseDTO> handleIndexingException(IndexingException exception) {
        log.warn("Ошибка бизнес-логики: {}", exception.getMessage());
        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setError(exception.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAllUncaughtException(Exception exception) {
        log.error("Произошла непредвиденная ошибка", exception);
        ErrorResponseDTO errorResponse = new ErrorResponseDTO();
        errorResponse.setError("На сервере произошла непредвиденная ошибка.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
