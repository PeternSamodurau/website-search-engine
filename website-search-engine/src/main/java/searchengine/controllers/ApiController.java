package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.component.ApiResponseFactory;
import searchengine.dto.response.SearchResponseDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final ApiResponseFactory apiResponseFactory; // <-- ВНЕДРЕНА ЗАВИСИМОСТЬ

    @GetMapping("/statistics")
    public ResponseEntity<?> statistics() {
        log.info("Получен запрос на статистику");
        try {
            StatisticsResponseDTO statistics = statisticsService.getStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Ошибка при получении статистики", e);
            return apiResponseFactory.createErrorResponse("Внутренняя ошибка сервера при получении статистики");
        }
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        log.info("Получен запрос на запуск индексации");
        if (indexingService.startIndexing()) {
            return apiResponseFactory.createSuccessResponse();
        } else {
            return apiResponseFactory.createErrorResponse("Индексация уже запущена");
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        log.info("Получен запрос на остановку индексации");
        if (indexingService.stopIndexing()) {
            return apiResponseFactory.createSuccessResponse();
        } else {
            return apiResponseFactory.createErrorResponse("Индексация не запущена");
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam(name = "url") String url) {
        log.info("Получен запрос на индексацию страницы: {}", url);
        if (url.isBlank()) {
            return apiResponseFactory.createErrorResponse("URL страницы не указан");
        }
        if (indexingService.indexPage(url)) {
            return apiResponseFactory.createSuccessResponse();
        } else {
            return apiResponseFactory.createErrorResponse("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponseDTO> search(@RequestParam(name = "query") String query,
                                                    @RequestParam(name = "site", required = false) String site,
                                                    @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                    @RequestParam(name = "limit", defaultValue = "20") int limit) {
        log.info("Получен поисковый запрос: query={}, site={}, offset={}, limit={}", query, site, offset, limit);
        SearchResponseDTO response = searchService.search(query, site, offset, limit);
        return ResponseEntity.ok(response);
    }
}
