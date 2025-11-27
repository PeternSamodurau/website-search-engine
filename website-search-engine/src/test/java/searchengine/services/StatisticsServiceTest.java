package searchengine.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private SitesListConfig sitesListConfig;

    @BeforeEach
    void setUp() {
        // Очистка репозиториев в правильном порядке
        indexRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();

        // Создание тестовых данных
        SiteConfig siteConfig1 = sitesListConfig.getSites().get(0);
        Site site1 = new Site();
        site1.setStatus(Status.INDEXED);
        site1.setStatusTime(LocalDateTime.now());
        site1.setLastError(null);
        site1.setUrl(siteConfig1.getUrl());
        site1.setName(siteConfig1.getName());
        siteRepository.save(site1);

        Page page1 = new Page(site1, "/path1", 200, "content1");
        Page page2 = new Page(site1, "/path2", 200, "content2");
        pageRepository.saveAll(List.of(page1, page2));

        Lemma lemma1 = new Lemma(site1, "лемма1", 2);
        Lemma lemma2 = new Lemma(site1, "лемма2", 1);
        lemmaRepository.saveAll(List.of(lemma1, lemma2));

        SiteConfig siteConfig2 = sitesListConfig.getSites().get(1);
        Site site2 = new Site();
        site2.setStatus(Status.INDEXING);
        site2.setStatusTime(LocalDateTime.now());
        site2.setLastError("Ошибка индексации");
        site2.setUrl(siteConfig2.getUrl());
        site2.setName(siteConfig2.getName());
        siteRepository.save(site2);

        Page page3 = new Page(site2, "/path3", 200, "content3");
        pageRepository.save(page3);

        Lemma lemma3 = new Lemma(site2, "лемма3", 1);
        lemmaRepository.save(lemma3);
    }

    @Test
    @DisplayName("Проверка получения статистики на основе тестовых данных")
    void getStatistics() {
        StatisticsResponseDTO response = statisticsService.getStatistics();

        // Проверки общего ответа
        assertNotNull(response);
        assertTrue(response.isResult());
        assertNotNull(response.getStatistics());

        // Проверки общей статистики (total)
        var total = response.getStatistics().getTotal();
        assertNotNull(total);
        assertEquals(2, total.getSites());
        assertEquals(3, total.getPages());
        assertEquals(3, total.getLemmas());
        assertFalse(total.isIndexing()); // По умолчанию индексация не идет

        // Проверки детальной статистики (detailed)
        var detailed = response.getStatistics().getDetailed();
        assertNotNull(detailed);
        assertEquals(2, detailed.size());

        // Проверка статистики для первого сайта
        var site1Stats = detailed.stream().filter(s -> s.getName().equals(sitesListConfig.getSites().get(0).getName())).findFirst().orElse(null);
        assertNotNull(site1Stats);
        assertEquals(sitesListConfig.getSites().get(0).getUrl(), site1Stats.getUrl());
        assertEquals(Status.INDEXED.toString(), site1Stats.getStatus());
        assertEquals(2, site1Stats.getPages());
        assertEquals(2, site1Stats.getLemmas());
        assertEquals("Ошибок нет", site1Stats.getError());

        // Проверка статистики для второго сайта
        var site2Stats = detailed.stream().filter(s -> s.getName().equals(sitesListConfig.getSites().get(1).getName())).findFirst().orElse(null);
        assertNotNull(site2Stats);
        assertEquals(sitesListConfig.getSites().get(1).getUrl(), site2Stats.getUrl());
        assertEquals(Status.INDEXING.toString(), site2Stats.getStatus());
        assertEquals(1, site2Stats.getPages());
        assertEquals(1, site2Stats.getLemmas());
        assertEquals("Ошибка индексации", site2Stats.getError());
    }
}
