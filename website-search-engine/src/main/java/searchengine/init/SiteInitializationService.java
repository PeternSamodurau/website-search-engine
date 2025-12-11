package searchengine.init;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Profile("init")
@Slf4j
public class SiteInitializationService {

    private final SitesListConfig sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @PostConstruct
    @Transactional
    public void initializeSites() {
        log.info("Запуск инициализации/обновления сайтов...");

        List<Site> sitesInDb = siteRepository.findAll();
        for (Site siteInDb : sitesInDb) {
            boolean foundInConfig = sites.getSites().stream()
                    .anyMatch(sc -> sc.getUrl().equals(siteInDb.getUrl()));
            if (!foundInConfig) {
                log.info("Удаление сайта из БД, отсутствующего в конфигурации: {}", siteInDb.getName());
                lemmaRepository.deleteAllBySite(siteInDb);
                pageRepository.deleteAllBySite(siteInDb);
                siteRepository.delete(siteInDb);
            }
        }

        for (SiteConfig siteConfig : sites.getSites()) {
            Site site = siteRepository.findByUrl(siteConfig.getUrl()).orElseGet(() -> {
                log.info("Создание нового сайта: {}", siteConfig.getName());
                Site newSite = new Site();
                newSite.setName(siteConfig.getName());
                newSite.setUrl(siteConfig.getUrl());
                return newSite;
            });

            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXING); // Первоначально устанавливаем статус INDEXING для всех сайтов
            site.setLastError(null);
            log.info("Сайт '{}' инициализирован со статусом: INDEXING", siteConfig.getName());

            siteRepository.save(site);
        }
        log.info("Инициализация/обновление сайтов завершено.");
    }
}