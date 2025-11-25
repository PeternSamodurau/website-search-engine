package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.CrawlerConfig;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    public static final AtomicBoolean isIndexing = new AtomicBoolean(false);

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    private final SitesListConfig sites;
    private final CrawlerConfig crawlerConfig;

    @Override
    public boolean startIndexing() {
        if (isIndexing.compareAndSet(false, true)) {
            log.info("Запуск процесса индексации");

            new Thread(() -> {
                try {
                    for (SiteConfig siteConfig : sites.getSites()) {
                        if (!isIndexing.get()) {
                            log.info("Индексация остановлена пользователем. Пропускаем оставшиеся сайты.");
                            break;
                        }
                        siteRepository.findByUrl(siteConfig.getUrl()).ifPresent(this::deleteSiteData);

                        Site site = new Site();
                        site.setUrl(siteConfig.getUrl());
                        site.setName(siteConfig.getName());
                        site.setStatus(Status.INDEXING);
                        site.setStatusTime(LocalDateTime.now());
                        site.setLastError(null);
                        siteRepository.save(site);

                        log.info("Запуск индексации для сайта: {}", site.getName());

                        ForkJoinPool forkJoinPool = new ForkJoinPool();
                        SiteCrawler.init();
                        SiteCrawler task = new SiteCrawler(site, site.getUrl(), crawlerConfig, pageRepository, lemmaService);
                        forkJoinPool.invoke(task);

                        Site updatedSite = siteRepository.findById(site.getId()).orElseThrow();
                        if (isIndexing.get()) {
                            updatedSite.setStatus(Status.INDEXED);
                            log.info("Индексация сайта '{}' успешно завершена.", updatedSite.getName());
                        } else {
                            updatedSite.setStatus(Status.FAILED);
                            updatedSite.setLastError("Индексация остановлена пользователем");
                            log.warn("Индексация сайта '{}' была остановлена.", updatedSite.getName());
                        }
                        updatedSite.setStatusTime(LocalDateTime.now());
                        siteRepository.save(updatedSite);
                    }
                } catch (Exception e) {
                    log.error("Критическая ошибка в главном потоке индексации", e);
                } finally {
                    isIndexing.set(false);
                    log.info("Процесс индексации ВСЕХ сайтов завершен.");
                }
            }, "Indexing-Manager-Thread").start();
            return true;
        } else {
            log.warn("Попытка запуска индексации, когда она уже запущена");
            return false;
        }
    }

    @Transactional
    public void deleteSiteData(Site site) {
        log.info("Очистка старых данных для сайта: {}", site.getName());
        List<Page> pages = pageRepository.findBySite(site);
        if (pages != null && !pages.isEmpty()) {
            log.info("Удаление {} индексов...", pages.size());
            pages.forEach(indexRepository::deleteByPage);
        }
        log.info("Удаление страниц...");
        pageRepository.deleteAllBySite(site);
        log.info("Удаление лемм...");
        lemmaRepository.deleteAllBySite(site);
        log.info("Удаление сайта...");
        siteRepository.delete(site);
    }

    @Override
    public boolean stopIndexing() {
        if (!isIndexing.get()) {
            log.warn("Попытка остановить индексацию, когда она не запущена");
            return false;
        }
        log.info("Остановка процесса индексации...");
        isIndexing.set(false);
        siteRepository.findAllByStatus(Status.INDEXING).forEach(site -> {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем.");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });
        return true;
    }

    @Override
    public boolean indexPage(String url) {
        log.warn("Метод indexPage в данной реализации не поддерживается");
        return false;
    }

    @Override
    public boolean isIndexing() {
        return isIndexing.get();
    }
}
