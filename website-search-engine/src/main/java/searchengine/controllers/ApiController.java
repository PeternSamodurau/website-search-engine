package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    public ResponseEntity<SuccessResponseDTO> startIndexing() {
        log.info("Получен запрос на запуск индексации");
        indexingService.startIndexing();
        return ResponseEntity.ok(new SuccessResponseDTO());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<SuccessResponseDTO> stopIndexing() {
        log.info("Получен запрос на остановку индексации");
        indexingService.stopIndexing();
        return ResponseEntity.ok(new SuccessResponseDTO());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<SuccessResponseDTO> indexPage(@RequestParam(name = "url") String url) {
        log.info("Получен запрос на индексацию страницы: {}", url);
        indexingService.indexPage(url);
        return ResponseEntity.ok(new SuccessResponseDTO());
    }
}
