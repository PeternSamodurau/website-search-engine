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
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class LemmaServiceTest {

    @Autowired private IndexingService indexingService;
    @Autowired private SiteRepository siteRepository;
    @Autowired private PageRepository pageRepository;
    @Autowired private LemmaRepository lemmaRepository;
    @Autowired private IndexRepository indexRepository;

    @MockBean
    private SitesListConfig sitesListConfig;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() throws IOException {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();

        SiteConfig siteConfig = new SiteConfig();
        siteConfig.setUrl(wireMockServer.baseUrl());
        siteConfig.setName("Test Site");
        when(sitesListConfig.getSites()).thenReturn(Collections.singletonList(siteConfig));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Проверка лемматизации ОДНОЙ страницы: корректные lemmas, rank и frequency")
    void lemmatizeSinglePage_ShouldProduceCorrectRanksAndFrequency() throws InterruptedException {
        // 1. ARRANGE
        stubFor(get(urlEqualTo("/")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/index.html"))));
        stubFor(get(urlEqualTo("/page2")).willReturn(aResponse().withStatus(404)));

        // 2. ACT
        indexingService.startIndexing();
        waitForIndexingToComplete();

        // 3. ASSERT
        // ИСПРАВЛЕНО: Используем findByUrl вместо выдуманного findByName
        Site site = siteRepository.findByUrl(wireMockServer.baseUrl()).orElseThrow();
        List<Lemma> actualLemmas = lemmaRepository.findBySite(site);
        int expectedLemmaCount = 15;
        log.info("Проверка количества лемм. Ожидали: {}, Получили: {}", expectedLemmaCount, actualLemmas.size());
        assertEquals(expectedLemmaCount, actualLemmas.size());

        Lemma leopardLemma = lemmaRepository.findByLemmaAndSite("леопард", site).orElseThrow();
        int expectedFrequency = 1;
        log.info("Проверка frequency для 'леопард'. Ожидали: {}, Получили: {}", expectedFrequency, leopardLemma.getFrequency());
        assertEquals(expectedFrequency, leopardLemma.getFrequency());

        // ИСПРАВЛЕНО: Используем findByPathAndSite вместо выдуманного findByPathAndSiteId
        Page page = pageRepository.findByPathAndSite("/", site).orElseThrow();
        Index leopardIndex = indexRepository.findByLemmaAndPage(leopardLemma, page).orElseThrow();
        float expectedRank = 2.0f;
        log.info("Проверка rank для 'леопард'. Ожидали: {}, Получили: {}", expectedRank, leopardIndex.getRank());
        assertEquals(expectedRank, leopardIndex.getRank());
    }

    @Test
    @DisplayName("Проверка лемматизации ТРЕХ страниц: корректные lemmas, rank и frequency")
    void lemmatizeThreePages_ShouldSumFrequencies() throws InterruptedException {
        // 1. ARRANGE
        stubFor(get(urlEqualTo("/")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/index.html"))));
        stubFor(get(urlEqualTo("/page2")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/page2.html"))));
        stubFor(get(urlEqualTo("/page3")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/page3.html"))));

        // 2. ACT
        indexingService.startIndexing();
        waitForIndexingToComplete();

        // 3. ASSERT
        // ИСПРАВЛЕНО: Используем findByUrl вместо выдуманного findByName
        Site site = siteRepository.findByUrl(wireMockServer.baseUrl()).orElseThrow();
        List<Lemma> actualLemmas = lemmaRepository.findBySite(site);
        int expectedLemmaCount = 16;
        log.info("Проверка количества уникальных лемм. Ожидали: {}, Получили: {}", expectedLemmaCount, actualLemmas.size());
        assertEquals(expectedLemmaCount, actualLemmas.size());

        Lemma leopardLemma = lemmaRepository.findByLemmaAndSite("леопард", site).orElseThrow();
        int expectedFrequency = 3;
        log.info("Проверка суммарной frequency для 'леопард'. Ожидали: {}, Получили: {}", expectedFrequency, leopardLemma.getFrequency());
        assertEquals(expectedFrequency, leopardLemma.getFrequency());

        long actualIndexCount = indexRepository.count();
        long expectedIndexCount = 45; // 15 + 15 + 15
        log.info("Проверка общего количества индексов. Ожидали: {}, Получили: {}", expectedIndexCount, actualIndexCount);
        assertEquals(expectedIndexCount, actualIndexCount);
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
        log.info("Индексация завершена.");
    }

    private String readTestResource(String path) {
        try {
            return Files.readString(Paths.get("src/test/resources/" + path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Не удалось прочитать тестовый ресурс: {}", path, e);
            return "";
        }
    }
}