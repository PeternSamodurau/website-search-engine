package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.response.ErrorResponseDTO;
import searchengine.dto.response.SuccessResponseDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponseDTO> statistics() {
        log.info("Получен запрос на статистику");
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        log.info("Получен запрос на запуск индексации");
        if (indexingService.startIndexing()) {
            return ResponseEntity.ok(new SuccessResponseDTO("Индексация запущена"));
        } else {
            return new ResponseEntity<>(new ErrorResponseDTO("Индексация уже запущена"), HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        log.info("Получен запрос на остановку индексации");
        if (indexingService.stopIndexing()) {
            return ResponseEntity.ok(new SuccessResponseDTO("Индексация остановлена"));
        } else {
            return new ResponseEntity<>(new ErrorResponseDTO("Индексация не запущена"), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam(name = "url") String url) {
        log.info("Получен запрос на индексацию страницы: {}", url);
        if (indexingService.indexPage(url)) {
            return ResponseEntity.ok(new SuccessResponseDTO("Страница поставлена в очередь на индексацию"));
        } else {
            return new ResponseEntity<>(new ErrorResponseDTO("Данная страница находится за пределами сайтов, указанных в конфигурационном файле"), HttpStatus.BAD_REQUEST);
        }
    }
}