package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class LemmaServiceTest {

    @Autowired private LemmaService lemmaService;
    @Autowired private SiteRepository siteRepository;
    @Autowired private PageRepository pageRepository;
    @Autowired private LemmaRepository lemmaRepository;
    @Autowired private IndexRepository indexRepository;

    private String pageText;

    @BeforeEach
    void setUp() throws IOException {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();

        pageText = readTestResource("test-site/index.html");
    }

    @Test
    @Transactional
    @DisplayName("Проверка лемматизации ОДНОЙ страницы: корректные lemmas, rank и frequency")
    void lemmatizeSinglePage_ShouldProduceCorrectRanksAndFrequency() {
        // 1. ARRANGE
        Site site = createAndSaveSite();
        Page page = createAndSavePage(site, "/index.html", pageText);

        // 2. ACT
        lemmaService.lemmatizePage(page);

        // 3. ASSERT
        List<Lemma> actualLemmas = lemmaRepository.findBySite(site);
        int expectedLemmaCount = 12;
        log.info("Проверка количества лемм. Ожидали: {}, Получили: {}", expectedLemmaCount, actualLemmas.size());
        assertEquals(expectedLemmaCount, actualLemmas.size());

        Lemma leopardLemma = lemmaRepository.findByLemmaAndSite("леопард", site).orElseThrow();
        int expectedFrequency = 1;
        log.info("Проверка frequency для 'леопард'. Ожидали: {}, Получили: {}", expectedFrequency, leopardLemma.getFrequency());
        assertEquals(expectedFrequency, leopardLemma.getFrequency());

        Index leopardIndex = indexRepository.findByLemmaAndPage(leopardLemma, page).orElseThrow();
        float expectedRank = 2.0f;
        log.info("Проверка rank для 'леопард'. Ожидали: {}, Получили: {}", expectedRank, leopardIndex.getRank());
        assertEquals(expectedRank, leopardIndex.getRank());
    }

    @Test
    @Transactional
    @DisplayName("Проверка лемматизации ТРЕХ страниц: корректные lemmas, rank и frequency")
    void lemmatizeThreePages_ShouldSumFrequencies() {
        // 1. ARRANGE
        Site site = createAndSaveSite();
        Page page1 = createAndSavePage(site, "/index.html", pageText);
        Page page2 = createAndSavePage(site, "/page2.html", pageText);
        Page page3 = createAndSavePage(site, "/page3.html", pageText);

        // 2. ACT
        lemmaService.lemmatizePage(page1);
        lemmaService.lemmatizePage(page2);
        lemmaService.lemmatizePage(page3);

        // 3. ASSERT
        List<Lemma> actualLemmas = lemmaRepository.findBySite(site);
        int expectedLemmaCount = 12;
        log.info("Проверка количества уникальных лемм. Ожидали: {}, Получили: {}", expectedLemmaCount, actualLemmas.size());
        assertEquals(expectedLemmaCount, actualLemmas.size());

        Lemma leopardLemma = lemmaRepository.findByLemmaAndSite("леопард", site).orElseThrow();
        int expectedFrequency = 3;
        log.info("Проверка суммарной frequency для 'леопард'. Ожидали: {}, Получили: {}", expectedFrequency, leopardLemma.getFrequency());
        assertEquals(expectedFrequency, leopardLemma.getFrequency());

        long actualIndexCount = indexRepository.count();
        long expectedIndexCount = 36;
        log.info("Проверка общего количества индексов. Ожидали: {}, Получили: {}", expectedIndexCount, actualIndexCount);
        assertEquals(expectedIndexCount, actualIndexCount);
    }

    // --- Вспомогательные методы ---
    private Site createAndSaveSite() {
        Site site = new Site();
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError("");
        site.setUrl("http://test.com");
        site.setName("Test Site");
        return siteRepository.save(site);
    }

    private Page createAndSavePage(Site site, String path, String content) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(path);
        page.setCode(200);
        page.setContent(content);
        return pageRepository.save(page);
    }

    private String readTestResource(String path) throws IOException {
        Path resourcePath = Path.of("src", "test", "resources", path);
        return Files.readString(resourcePath, StandardCharsets.UTF_8);
    }
}
