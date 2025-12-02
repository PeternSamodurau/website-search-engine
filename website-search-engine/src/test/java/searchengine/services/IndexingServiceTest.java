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
import java.util.Collections;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class IndexingServiceTest {

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

    @MockBean
    private SitesListConfig sitesListConfig;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() throws IOException {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        // Теперь база данных сама обрабатывает каскадное удаление благодаря аннотациям @OnDelete.
        // Достаточно удалить только сайты.
        siteRepository.deleteAll();

        SiteConfig siteConfig = new SiteConfig();
        siteConfig.setUrl(wireMockServer.baseUrl());
        siteConfig.setName("Test Site");
        when(sitesListConfig.getSites()).thenReturn(Collections.singletonList(siteConfig));

        stubFor(get(urlEqualTo("/")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/index.html"))));
        stubFor(get(urlEqualTo("/page2")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/page2.html"))));
        stubFor(get(urlEqualTo("/page3")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/page3.html"))));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Успешная индексация: сервис должен найти и сохранить все 3 страницы с тестового сайта.")
    void shouldIndexAllPagesFromTestSite() throws InterruptedException {
        boolean result = indexingService.startIndexing();
        assertTrue(result, "Запуск индексации должен вернуть true");

        waitForIndexingToComplete();

        long expectedPageCount = 3;
        long actualPageCount = pageRepository.count();
        log.info("Проверка результата: ожидается {}, в базе данных найдено: {}", expectedPageCount, actualPageCount);
        assertEquals(expectedPageCount, actualPageCount, "Количество проиндексированных страниц должно быть равно 3.");
    }

    @Test
    @DisplayName("Повторная индексация: при повторном запуске сервис должен сначала удалить старые данные, а затем проиндексировать сайт заново. Итоговое количество страниц не должно измениться.")
    void shouldReIndexSiteCorrectly() throws InterruptedException {
        log.info("Тест повторной индексации: запуск первого прохода...");
        indexingService.startIndexing();
        waitForIndexingToComplete();

        long countAfterFirstRun = pageRepository.count();
        assertEquals(3, countAfterFirstRun, "После первого запуска должно быть 3 страницы.");
        log.info("Первый проход завершен. В базе {} страниц.", countAfterFirstRun);

        log.info("Тест повторной индексации: запуск второго прохода...");
        indexingService.startIndexing();
        waitForIndexingToComplete();

        long countAfterSecondRun = pageRepository.count();
        log.info("Второй проход завершен. В базе {} страниц.", countAfterSecondRun);
        assertEquals(3, countAfterSecondRun, "После повторной индексации количество страниц не должно измениться.");
    }

    @Test
    @DisplayName("Остановка индексации: при вызове stopIndexing() процесс должен быть прерван, в результате чего в базе сохранится меньше страниц, чем есть на сайте.")
    void shouldStopIndexingMidway() throws InterruptedException {
        log.info("Тест остановки индексации: запуск...");
        indexingService.startIndexing();

        // Ждем, пока индексация начнется (статус INDEXING)
        Site testSite = waitForSiteStatus(wireMockServer.baseUrl(), Status.INDEXING, 10);
        assertNotNull(testSite, "Сайт должен перейти в статус INDEXING.");
        log.info("Индексация сайта '{}' началась (статус: {}).", testSite.getName(), testSite.getStatus());

        // Даем немного времени для индексации нескольких страниц
        Thread.sleep(500); 

        log.info("Отправка команды на остановку...");
        boolean stopResult = indexingService.stopIndexing();
        assertTrue(stopResult, "Остановка индексации должна вернуть true");

        // Ждем, пока индексация полностью остановится (статус FAILED или STOPPED)
        testSite = waitForSiteStatusNot(wireMockServer.baseUrl(), Status.INDEXING, 10);
        assertNotNull(testSite, "Сайт должен перестать быть в статусе INDEXING.");
        log.info("Индексация сайта '{}' завершена (статус: {}).", testSite.getName(), testSite.getStatus());
        
        long actualPageCount = pageRepository.count();
        log.info("Индексация остановлена. В базе найдено {} страниц.", actualPageCount);
        assertTrue(actualPageCount < 3 && actualPageCount > 0, "Количество страниц должно быть больше 0, но меньше 3, если остановка прошла успешно.");
    }

    @Test
    @DisplayName("Обработка ошибок сети: если одна из страниц возвращает ошибку (500), сервис должен пропустить ее, залогировать ошибку и продолжить работу, не падая.")
    void shouldHandleSiteErrorGracefully() throws InterruptedException {
        log.info("Тест обработки ошибок: настройка мока на возврат ошибки 500...");
        stubFor(get(urlEqualTo("/page2")).willReturn(aResponse().withStatus(500)));

        indexingService.startIndexing();
        waitForIndexingToComplete();

        long actualPageCount = pageRepository.count();
        log.info("Индексация с ошибкой завершена. В базе найдено {} страниц.", actualPageCount);
        assertEquals(1, actualPageCount, "Должна быть проиндексирована только одна страница, остальные должны быть пропущены из-за ошибки.");
    }

    @Test
    @DisplayName("Индексация одной страницы: сервис должен корректно индексировать одну указанную страницу.")
    void shouldIndexSinglePageCorrectly() {
        // 1. Действие
        boolean result = indexingService.indexPage(wireMockServer.baseUrl() + "/page2");
        assertTrue(result, "Индексация отдельной страницы должна вернуть true");

        // 2. Проверка (только для Page)
        assertEquals(1, pageRepository.count(), "В базе должна быть одна страница.");
        assertEquals("/page2", pageRepository.findAll().get(0).getPath(), "Путь сохраненной страницы должен быть /page2.");
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

    private Site waitForSiteStatus(String url, Status expectedStatus, int maxWaitSeconds) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < maxWaitSeconds * 1000) {
            Optional<Site> siteOptional = siteRepository.findByUrl(url);
            if (siteOptional.isPresent() && siteOptional.get().getStatus() == expectedStatus) {
                return siteOptional.get();
            }
            Thread.sleep(500); // Опрашиваем каждые 500 мс
        }
        return siteRepository.findByUrl(url).orElse(null); // Возвращаем текущее состояние или null
    }

    private Site waitForSiteStatusNot(String url, Status unexpectedStatus, int maxWaitSeconds) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < maxWaitSeconds * 1000) {
            Optional<Site> siteOptional = siteRepository.findByUrl(url);
            if (siteOptional.isPresent() && siteOptional.get().getStatus() != unexpectedStatus) {
                return siteOptional.get();
            }
            Thread.sleep(500); // Опрашиваем каждые 500 мс
        }
        return siteRepository.findByUrl(url).orElse(null); // Возвращаем текущее состояние или null
    }

    private String readTestResource(String path) throws IOException {
        return Files.readString(Paths.get("src/test/resources/" + path), StandardCharsets.UTF_8);
    }
}
