package searchengine.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesListConfig;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteDataCleaner {

    private final SiteRepository siteRepository;
    private final SitesListConfig sites;

    @Transactional
    public void clearDataForSite(Site site) {
        log.info("Полное удаление данных для сайта: {}. Доверяем каскадному удалению в БД.", site.getName());
        siteRepository.delete(site);
        log.info("Удаление сайта {} и всех связанных данных завершено.", site.getName());
    }
}
