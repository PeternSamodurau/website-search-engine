package searchengine.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SiteDataCleaner {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesListConfig sites;

    @Transactional
    public void clearSitesData() {
        log.info("Фаза 1: Транзакционная очистка старых данных...");
        for (SiteConfig siteConfig : sites.getSites()) {
            siteRepository.findByUrl(siteConfig.getUrl()).ifPresent(site -> {
                log.info("Удаляется сайт: {}", site.getName());

                // 1. Удаляем все записи из таблицы 'index', связанные с этим сайтом
                //    Для этого сначала находим все страницы, принадлежащие сайту.
                List<Page> pagesToDelete = pageRepository.findBySite(site);
                if (!pagesToDelete.isEmpty()) {
                    log.info("Удаление {} записей из 'index' для сайта {}", indexRepository.countByPageIn(pagesToDelete), site.getName());
                    indexRepository.deleteByPageIn(pagesToDelete);
                }

                // 2. Удаляем все записи из таблицы 'page', связанные с этим сайтом
                if (!pagesToDelete.isEmpty()) {
                    log.info("Удаление {} записей из 'page' для сайта {}", pagesToDelete.size(), site.getName());
                    pageRepository.deleteAll(pagesToDelete);
                }

                // 3. Удаляем все записи из таблицы 'lemma', связанные с этим сайтом
                List<Lemma> lemmasToDelete = lemmaRepository.findBySite(site);
                if (!lemmasToDelete.isEmpty()) {
                    log.info("Удаление {} записей из 'lemma' для сайта {}", lemmasToDelete.size(), site.getName());
                    lemmaRepository.deleteAll(lemmasToDelete);
                }

                // 4. Удаляем сам сайт
                siteRepository.delete(site);
            });
        }
        log.info("Фаза 1: Очистка завершена. Транзакция будет закоммичена.");
    }
}