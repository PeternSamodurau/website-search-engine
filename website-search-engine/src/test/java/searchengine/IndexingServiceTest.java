package searchengine;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import searchengine.config.CrawlerConfig;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.LemmaService;
import searchengine.services.SiteCrawler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class IndexingServiceTest {

    // 2. Определяем контейнер PostgreSQL, который будет запущен для тестов
    @Container
    @ServiceConnection // 3. Spring Boot автоматически подключится к этому контейнеру
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private CrawlerConfig crawlerConfig;

    @Autowired
    private LemmaService lemmaService;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() throws IOException {
        // Запускаем WireMock на случайном свободном порту
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        // Полностью очищаем таблицы перед каждым тестом для изоляции.
        pageRepository.deleteAll();
        siteRepository.deleteAll();

        // Настраиваем WireMock, чтобы он отдавал содержимое наших файлов
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
        IndexingServiceImpl.isIndexing.set(false); // На всякий случай сбрасываем флаг после теста
    }

    @Test
    void testSiteCrawling() {
        // 1. Подготовка: создаем и сохраняем сайт, который будем индексировать
        String rootUrl = wireMockServer.baseUrl();
        Site site = new Site();
        site.setUrl(rootUrl);
        site.setName("Test Site");
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);

        // 2. Действие: инициализируем и запускаем краулер
        SiteCrawler.init(); // Сбрасываем кеш посещенных ссылок
        IndexingServiceImpl.isIndexing.set(true); // <-- ВКЛЮЧАЕМ ИНДЕКСАЦИЮ ДЛЯ ТЕСТА

        ForkJoinPool forkJoinPool = new ForkJoinPool();

        // Получаем управляемую JPA-сущность сайта из БД
        Site managedSite = siteRepository.findByUrl(rootUrl).orElseThrow();

        SiteCrawler task = new SiteCrawler(managedSite, rootUrl, crawlerConfig, pageRepository, lemmaService);
        forkJoinPool.invoke(task);

        // 3. Проверка: убеждаемся, что в базе данных ровно 3 страницы
        long expectedPageCount = 3;
        long actualPageCount = pageRepository.count();
        assertEquals(expectedPageCount, actualPageCount, "Количество проиндексированных страниц должно быть равно 3.");
    }

    // Вспомогательный метод для чтения файлов из тестовых ресурсов
    private String readTestResource(String path) throws IOException {
        return Files.readString(Paths.get("src/test/resources/" + path), StandardCharsets.UTF_8);
    }
}
