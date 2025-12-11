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
        siteConfig.setEnabled(true);
        when(sitesListConfig.getSites()).thenReturn(Collections.singletonList(siteConfig));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Проверка лемматизации ОДНОЙ страницы: корректные lemmas, rank и frequency")
    void lemmatizeSinglePage_ShouldProduceCorrectRanksAndFrequency() throws InterruptedException {

        stubFor(get(urlEqualTo("/")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/index.html"))));
        stubFor(get(urlEqualTo("/page2")).willReturn(aResponse().withStatus(404)));

        indexingService.startIndexing();
        waitForIndexingToComplete();

        Site site = siteRepository.findByUrl(wireMockServer.baseUrl()).orElseThrow();
        List<Lemma> actualLemmas = lemmaRepository.findBySite(site);
        assertEquals(15, actualLemmas.size());

        Lemma leopardLemma = lemmaRepository.findByLemmaAndSite("леопард", site).orElseThrow();
        assertEquals(1, leopardLemma.getFrequency());

        Page page = pageRepository.findByPathAndSite("/", site).orElseThrow();
        Index leopardIndex = indexRepository.findByLemmaAndPage(leopardLemma, page).orElseThrow();
        assertEquals(2.0f, leopardIndex.getRank());
    }

    @Test
    @DisplayName("Проверка лемматизации ТРЕХ страниц: корректные lemmas, rank и frequency")
    void lemmatizeThreePages_ShouldSumFrequencies() throws InterruptedException {

        stubFor(get(urlEqualTo("/")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/index.html"))));
        stubFor(get(urlEqualTo("/page2")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/page2.html"))));
        stubFor(get(urlEqualTo("/page3")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody(readTestResource("test-site/page3.html"))));

        indexingService.startIndexing();
        waitForIndexingToComplete();

        Site site = siteRepository.findByUrl(wireMockServer.baseUrl()).orElseThrow();
        List<Lemma> actualLemmas = lemmaRepository.findBySite(site);
        assertEquals(16, actualLemmas.size());

        Lemma leopardLemma = lemmaRepository.findByLemmaAndSite("леопард", site).orElseThrow();
        assertEquals(3, leopardLemma.getFrequency());

        assertEquals(45, indexRepository.count());
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