package searchengine.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.component.ApiResponseFactory;
import searchengine.dto.search.SearchResponseDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "API поискового движка", description = "Операции для управления индексацией и выполнения поиска")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final ApiResponseFactory apiResponseFactory;

    @GetMapping("/statistics")
    @Operation(
            summary = "Получение статистики",
            description = "Возвращает статистику по всем сайтам: общее количество сайтов, страниц, лемм и детальную информацию по каждому сайту."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика успешно получена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StatisticsResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"result\": false, \"error\": \"Внутренняя ошибка сервера при получении статистики\"}")))
    })
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
    @Operation(
            summary = "Запуск индексации",
            description = "Запускает процесс полной индексации всех сайтов, указанных в конфигурации. Процесс включает удаление старых данных и обход сайтов."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос обработан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"result\": true} или {\"result\": false, \"error\": \"Индексация уже запущена\"}")))
    })
    public ResponseEntity<Map<String, Object>> startIndexing() {
        log.info("Получен запрос на запуск индексации");
        if (indexingService.startIndexing()) {
            return apiResponseFactory.createSuccessResponse();
        } else {
            return apiResponseFactory.createErrorResponse("Индексация уже запущена");
        }
    }

    @GetMapping("/stopIndexing")
    @Operation(
            summary = "Остановка индексации",
            description = "Останавливает текущий процесс индексации. Уже проиндексированные данные сохраняются."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос обработан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"result\": true} или {\"result\": false, \"error\": \"Индексация не запущена\"}")))
    })
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        log.info("Получен запрос на остановку индексации");
        if (indexingService.stopIndexing()) {
            return apiResponseFactory.createSuccessResponse();
        } else {
            return apiResponseFactory.createErrorResponse("Индексация не запущена");
        }
    }

    @PostMapping("/indexPage")
    @Operation(
            summary = "Индексация отдельной страницы",
            description = "Добавляет или обновляет в индексе одну указанную страницу. Страница должна принадлежать одному из сайтов, указанных в конфигурации."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос обработан",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\"result\": true} или {\"result\": false, \"error\": \"...\"}")))
    })
    public ResponseEntity<Map<String, Object>> indexPage(
            @Parameter(description = "Полный URL страницы для индексации", required = true, example = "http://example.com/page")
            @RequestParam(name = "url") String url) {
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
    @Operation(
            summary = "Поиск по сайтам",
            description = "Выполняет поиск по проиндексированным сайтам. Возвращает список страниц, релевантных поисковому запросу."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Поиск успешно выполнен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SearchResponseDTO.class)))
    })
    public ResponseEntity<SearchResponseDTO> search(
            @Parameter(description = "Поисковый запрос", required = true, example = "программирование на java")
            @RequestParam(name = "query") String query,
            @Parameter(description = "Сайт для поиска (если не указан, поиск по всем сайтам)", example = "http://example.com")
            @RequestParam(name = "site", required = false) String site,
            @Parameter(description = "Смещение для пагинации (количество результатов, которые нужно пропустить)")
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @Parameter(description = "Максимальное количество результатов в выдаче")
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        log.info("Получен поисковый запрос: query={}, site={}, offset={}, limit={}", query, site, offset, limit);
        SearchResponseDTO response = searchService.search(query, site, offset, limit);
        return ResponseEntity.ok(response);
    }
}
