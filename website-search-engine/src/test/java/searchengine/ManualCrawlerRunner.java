package searchengine;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import searchengine.config.CrawlerConfig;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.LemmaService;
import searchengine.services.SiteCrawler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class ManualCrawlerRunner {

    public static void main(String[] args) {

        // --- ОТКЛЮЧАЕМ ПРОФИЛЬ 'init', ЧТОБЫ ПРИЛОЖЕНИЕ НЕ ВИСЛО ---
        SpringApplication app = new SpringApplication(WebsiteSearchEngineApplication.class);
        app.setDefaultProperties(Collections.singletonMap("spring.profiles.active", ""));
        ConfigurableApplicationContext context = app.run(args);
        // -----------------------------------------------------------

        WireMockServer wireMockServer = null;
        IndexingServiceImpl.isIndexing.set(true);
        PageRepository pageRepository = null;

        try {
            // --- 1. Настройка и запуск WireMock сервера ---
            wireMockServer = new WireMockServer(8089);
            wireMockServer.start();
            WireMock.configureFor("localhost", 8089);

            System.out.println("--- WireMock Server Started on port 8089 ---");

            // --- 2. Определение "виртуальных" страниц для краулера ---
            stubFor(get(urlEqualTo("/"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "text/html")
                            .withBody("<!DOCTYPE html><html><head><title>Главная</title></head><body>" +
                                    "<p>Главная страница. Ссылка на page2.</p>" +
                                    "<a href=\"/page2\">Вторая страница</a></body></html>")));

            stubFor(get(urlEqualTo("/page2"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "text/html")
                            .withBody("<!DOCTYPE html><html><head><title>Вторая</title></head><body>" +
                                    "<h1>Вторая страница</h1>" +
                                    "<a href=\"/\">Ссылка на главную</a><br>" +
                                    "<a href=\"/page3\">Ссылка на третью</a>" +
                                    "</body></html>")));

            stubFor(get(urlEqualTo("/page3"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "text/html")
                            .withBody("<!DOCTYPE html><html><head><title>Третья</title></head><body>" +
                                    "<p>Третья страница. Ссылка на главную.</p>" +
                                    "<a href=\"/\">Главная</a>" +
                                    "<!-- Невалидные ссылки для теста -->" +
                                    "<a href=\"/page2#comments\">Якорь</a>" +
                                    "<a href=\"/files/document.pdf\">PDF файл</a>" +
                                    "<a href=\"https://www.google.com\">Внешний сайт</a>" +
                                    "<a href=\"\">Пустая ссылка</a>" +
                                    "</body></html>")));

            // --- 3. Подготовка к запуску вашего краулера на "виртуальном" сайте ---
            String targetUrl = "http://localhost:8089/";

            // Получаем необходимые бины из Spring контекста
            CrawlerConfig crawlerConfig = context.getBean(CrawlerConfig.class);
            pageRepository = context.getBean(PageRepository.class);
            LemmaService lemmaService = context.getBean(LemmaService.class);
            SiteRepository siteRepository = context.getBean(SiteRepository.class);

            // !!! ВАЖНО: Инициализируем SiteCrawler (очищаем visitedUrls) !!!
            SiteCrawler.init();

            // Создаем объект Site для нашего виртуального сайта
            Site site = new Site();
            site.setUrl(targetUrl);
            site.setName("Virtual Test Site");
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(null);

            // Сохраняем сайт в базу данных (методы репозитория транзакционны по умолчанию)
            site = siteRepository.save(site);

            System.out.println("--- Starting crawl for: " + targetUrl + " ---");

            ForkJoinPool forkJoinPool = new ForkJoinPool();
            SiteCrawler task = new SiteCrawler(site, targetUrl, crawlerConfig, pageRepository, lemmaService);

            // Запускаем задачу в ForkJoinPool
            forkJoinPool.invoke(task);

            System.out.println("--- Crawl finished for: " + targetUrl + " ---");

        } catch (Exception e) {
            System.err.println("An error occurred during crawling: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // --- 4. Остановка WireMock сервера и очистка ---
            if (wireMockServer != null) {
                wireMockServer.stop();
                System.out.println("--- WireMock Server Stopped ---");
            }

            // --- БЛОК ПРОВЕРКИ РЕЗУЛЬТАТА ---
            if (pageRepository != null) {
                System.out.println("\n--- TEST SUMMARY ---");
                // Мы не можем гарантировать точное число, если не чистим базу,
                // поэтому просто выведем фактическое количество.
                long actualPageCount = pageRepository.count();
                System.out.println("Actual indexed pages in DB: " + actualPageCount);
                System.out.println("--------------------\n");
            }

            // Сбрасываем флаг индексации
            IndexingServiceImpl.isIndexing.set(false);
            // Закрываем Spring Boot контекст
            context.close();
            System.out.println("Spring application context closed.");
        }
    }
}