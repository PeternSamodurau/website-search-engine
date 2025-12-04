package searchengine.services;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.SiteStatisticsDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.dto.statistics.TotalStatisticsDTO;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StatisticsServiceTest {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private IndexingService indexingService;
    @Autowired
    private StatisticsService statisticsService;

    @MockBean
    private SitesListConfig sitesListConfig;

    private static WireMockServer wireMockServer;
    private StatisticsResponseDTO statisticsResponse;

    @BeforeAll
    static void startServer() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        log.info("--- Начало настройки теста ---");
        // 1. Очистка БД
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
        log.info("Репозитории очищены.");

        // 2. Настройка моков для двух сайтов
        SiteConfig siteConfig1 = new SiteConfig();
        siteConfig1.setUrl(wireMockServer.baseUrl());
        siteConfig1.setName("Test Site 1 (Indexed)");
        siteConfig1.setEnabled(true); // FIX: Allow indexing for this site

        SiteConfig siteConfig2 = new SiteConfig();
        siteConfig2.setUrl("http://example.com");
        siteConfig2.setName("Test Site 2 (Empty)");
        siteConfig2.setEnabled(false); // FIX: Explicitly disable indexing for this site

        when(sitesListConfig.getSites()).thenReturn(Arrays.asList(siteConfig1, siteConfig2));
        log.info("Мок для SitesListConfig настроен с 2 сайтами.");

        // 3. Настройка WireMock
        stubFor(get(urlEqualTo("/")).willReturn(aResponse().withHeader("Content-Type", "text/html").withBody(readTestResource("test-site/index.html"))));
        stubFor(get(urlEqualTo("/page2")).willReturn(aResponse().withHeader("Content-Type", "text/html").withBody(readTestResource("test-site/page2.html"))));
        stubFor(get(urlEqualTo("/page3")).willReturn(aResponse().withHeader("Content-Type", "text/html").withBody(readTestResource("test-site/page3.html"))));
        log.info("Заглушки WireMock настроены.");

        // 4. Добавляем второй сайт в БД вручную, чтобы он просто существовал для статистики
        Site emptySite = new Site();
        emptySite.setUrl(siteConfig2.getUrl());
        emptySite.setName(siteConfig2.getName());
        emptySite.setStatus(Status.INDEXED); // Предположим, он был проиндексирован ранее и пуст
        emptySite.setStatusTime(LocalDateTime.now());
        siteRepository.save(emptySite);
        log.info("Пустой сайт создан в БД.");

        // 5. Запуск и ожидание полной индексации (затронет только siteConfig1)
        log.info("Запуск полной индексации...");
        indexingService.startIndexing();
        waitForIndexingToComplete();
        log.info("Индексация завершена.");

        // 6. Получение статистики
        statisticsResponse = statisticsService.getStatistics();
        log.info("Статистика получена. --- Настройка теста завершена ---");
    }

    @Test
    @Order(1)
    @DisplayName("Проверка общей структуры ответа: должен быть успешным и содержать статистику по всем сайтам")
    void testGeneralResponse_ShouldBeSuccessfulAndContainAllSites() {
        log.info("Тест: Общая структура ответа");
        assertNotNull(statisticsResponse, "Ответ не должен быть null");

        log.info("Проверка поля 'result'. Ожидается: true, Фактически: {}", statisticsResponse.isResult());
        assertTrue(statisticsResponse.isResult(), "Поле 'result' должно быть true");
        assertNotNull(statisticsResponse.getStatistics(), "Блок 'statistics' не должен быть null");

        int expectedSiteCount = 2;
        int actualSiteCount = statisticsResponse.getStatistics().getDetailed().size();
        log.info("Проверка количества сайтов в 'detailed'. Ожидается: {}, Фактически: {}", expectedSiteCount, actualSiteCount);
        assertEquals(expectedSiteCount, actualSiteCount, "Количество сайтов в 'detailed' должно быть " + expectedSiteCount);
    }

    @Test
    @Order(2)
    @DisplayName("Проверка блока 'total': должен корректно суммировать данные со всех сайтов")
    void testTotalStatistics_ShouldCorrectlySumUpSitesPagesAndLemmas() {
        log.info("Тест: Блок общей статистики 'total'");
        TotalStatisticsDTO total = statisticsResponse.getStatistics().getTotal();
        assertNotNull(total, "Блок 'total' не должен быть null");

        int expectedSites = 2;
        log.info("Проверка общего количества сайтов. Ожидается: {}, Фактически: {}", expectedSites, total.getSites());
        assertEquals(expectedSites, total.getSites(), "Общее количество сайтов должно быть 2");

        int expectedPages = 3;
        log.info("Проверка общего количества страниц. Ожидается: {}, Фактически: {}", expectedPages, total.getPages());
        assertEquals(expectedPages, total.getPages(), "Общее количество страниц должно быть 3 (все со первого сайта)");

        int expectedLemmas = 16;
        log.info("Проверка общего количества лемм. Ожидается: {}, Фактически: {}", expectedLemmas, total.getLemmas());
        assertEquals(expectedLemmas, total.getLemmas(), "Общее количество лемм должно быть 16");

        log.info("Проверка статуса 'isIndexing'. Ожидается: false, Фактически: {}", total.isIndexing());
        assertFalse(total.isIndexing(), "Статус 'indexing' должен быть false после завершения");
    }

    @Test
    @Order(3)
    @DisplayName("Проверка детальной статистики для проиндексированного сайта")
    void testDetailedStatistics_ShouldShowCorrectDataForIndexedSite() {
        log.info("Тест: Детальная статистика для проиндексированного сайта");
        Optional<SiteStatisticsDTO> indexedSiteStatsOpt = findStatisticsByUrl(wireMockServer.baseUrl());
        assertTrue(indexedSiteStatsOpt.isPresent(), "Статистика для проиндексированного сайта должна присутствовать");

        SiteStatisticsDTO indexedSiteStats = indexedSiteStatsOpt.get();
        String expectedName = "Test Site 1 (Indexed)";
        log.info("Проверка имени проиндексированного сайта. Ожидается: '{}', Фактически: '{}'", expectedName, indexedSiteStats.getName());
        assertEquals(expectedName, indexedSiteStats.getName());

        Status expectedStatus = Status.INDEXED;
        log.info("Проверка статуса проиндексированного сайта. Ожидается: {}, Фактически: {}", expectedStatus.name(), indexedSiteStats.getStatus());
        assertEquals(expectedStatus.name(), indexedSiteStats.getStatus());

        int expectedPages = 3;
        log.info("Проверка количества страниц проиндексированного сайта. Ожидается: {}, Фактически: {}", expectedPages, indexedSiteStats.getPages());
        assertEquals(expectedPages, indexedSiteStats.getPages(), "Количество страниц для проиндексированного сайта должно быть 3");

        int expectedLemmas = 16;
        log.info("Проверка количества лемм проиндексированного сайта. Ожидается: {}, Фактически: {}", expectedLemmas, indexedSiteStats.getLemmas());
        assertEquals(expectedLemmas, indexedSiteStats.getLemmas(), "Количество лемм для проиндексированного сайта должно быть 16");

        assertNotNull(indexedSiteStats.getError(), "Поле ошибки не должно быть null");
    }

    @Test
    @Order(4)
    @DisplayName("Проверка детальной статистики для пустого (неиндексированного) сайта")
    void testDetailedStatistics_ShouldShowZeroCountsForEmptySite() {
        log.info("Тест: Детальная статистика для пустого сайта");
        Optional<SiteStatisticsDTO> emptySiteStatsOpt = findStatisticsByUrl("http://example.com");
        assertTrue(emptySiteStatsOpt.isPresent(), "Статистика для пустого сайта должна присутствовать");

        SiteStatisticsDTO emptySiteStats = emptySiteStatsOpt.get();
        String expectedName = "Test Site 2 (Empty)";
        log.info("Проверка имени пустого сайта. Ожидается: '{}', Фактически: '{}'", expectedName, emptySiteStats.getName());
        assertEquals(expectedName, emptySiteStats.getName());

        Status expectedStatus = Status.FAILED; // <--- FIX: The service now correctly marks a disabled site as FAILED.
        log.info("Проверка статуса пустого сайта. Ожидается: {}, Фактически: {}", expectedStatus.name(), emptySiteStats.getStatus());
        assertEquals(expectedStatus.name(), emptySiteStats.getStatus());

        int expectedPages = 0;
        log.info("Проверка количества страниц пустого сайта. Ожидается: {}, Фактически: {}", expectedPages, emptySiteStats.getPages());
        assertEquals(expectedPages, emptySiteStats.getPages(), "Количество страниц для пустого сайта должно быть 0");

        int expectedLemmas = 0;
        log.info("Проверка количества лемм пустого сайта. Ожидается: {}, Фактически: {}", expectedLemmas, emptySiteStats.getLemmas());
        assertEquals(expectedLemmas, emptySiteStats.getLemmas(), "Количество лемм для пустого сайта должно быть 0");
    }

    private Optional<SiteStatisticsDTO> findStatisticsByUrl(String url) {
        return statisticsResponse.getStatistics().getDetailed().stream()
                .filter(s -> s.getUrl().equals(url))
                .findFirst();
    }

    private void waitForIndexingToComplete() throws InterruptedException {
        int maxWaitTimeSeconds = 30;
        while (indexingService.isIndexing() && maxWaitTimeSeconds > 0) {
            Thread.sleep(1000);
            maxWaitTimeSeconds--;
        }
        if (indexingService.isIndexing()) {
            fail("Индексация не завершилась за 30 секунд.");
        }
    }

    private String readTestResource(String path) throws IOException {
        return Files.readString(Paths.get("src/test/resources/" + path), StandardCharsets.UTF_8);
    }
}