package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaService lemmaService;
    private final SitesListConfig sites;

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;

    @Override
    public boolean startIndexing() {
        if (isIndexing.compareAndSet(false, true)) {
            log.info("Запуск процесса индексации");

            new Thread(() -> {
                forkJoinPool = new ForkJoinPool();
                try {
                    Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

                    for (SiteConfig siteConfig : sites.getSites()) {
                        Site site = siteRepository.findByUrl(siteConfig.getUrl()).orElseGet(() -> {
                            Site newSite = new Site();
                            newSite.setUrl(siteConfig.getUrl());
                            newSite.setName(siteConfig.getName());
                            return newSite;
                        });

                        site.setStatus(Status.INDEXING);
                        site.setStatusTime(LocalDateTime.now());
                        site.setLastError(null);
                        siteRepository.save(site);

                        log.info("Очистка старых данных для сайта: {}", site.getName());
                        lemmaRepository.deleteAllBySite(site);
                        pageRepository.deleteAllBySite(site);
                    }

                    List<Site> sitesToProcess = siteRepository.findAllByStatus(Status.INDEXING);
                    if (sitesToProcess.isEmpty()) {
                        log.info("Нет сайтов со статусом INDEXING для обработки. Процесс индексации завершен.");
                        return;
                    }

                    for (Site site : sitesToProcess) {
                        if (!isIndexing.get()) {
                            log.info("Индексация остановлена пользователем. Пропускаем оставшиеся сайты.");
                            break;
                        }

                        Site currentSiteState = siteRepository.findByUrl(site.getUrl()).orElse(site);
                        if (currentSiteState.getStatus() == Status.FAILED) {
                            log.warn("Сайт '{}' уже имеет статус FAILED (из-за инициализации), пропуск индексации. Причина: {}", currentSiteState.getName(), currentSiteState.getLastError());
                            continue;
                        }

                        log.info("Запуск индексации для сайта: {}", site.getName());

                        SiteCrawler task = new SiteCrawler(site, site.getUrl(), this, siteRepository, pageRepository, lemmaService, visitedUrls);
                        forkJoinPool.invoke(task);

                        Site updatedSite = siteRepository.findByUrl(site.getUrl()).orElse(site);
                        if (updatedSite != null) {
                            if (updatedSite.getStatus() != Status.FAILED) {
                                updatedSite.setStatus(Status.INDEXED);
                                updatedSite.setLastError(null);
                                updatedSite.setStatusTime(LocalDateTime.now());
                                siteRepository.save(updatedSite);

                                log.info("Индексация сайта '{}' завершена.", updatedSite.getName());
                            } else {
                                log.warn("Индексация сайта '{}' завершена со статусом FAILED, установленным SiteCrawler'ом. Причина: {}", updatedSite.getName(), updatedSite.getLastError());
                            }
                        } else {
                            log.error("Не удалось найти сайт '{}' после индексации для обновления статуса.", site.getName());
                        }
                    }
                } catch (Exception e) {
                    log.error("Критическая ошибка во время процесса индексации", e);
                    siteRepository.findAllByStatus(Status.INDEXING).forEach(s -> {
                        s.setStatus(Status.FAILED);
                        s.setLastError("Критическая ошибка во время индексации: " + e.getMessage());
                        s.setStatusTime(LocalDateTime.now());
                        siteRepository.save(s);
                    });
                } finally {
                    if (forkJoinPool != null) {
                        forkJoinPool.shutdown();
                    }
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

    @Override
    public boolean stopIndexing() {
        if (!isIndexing.get()) {
            log.warn("Попытка остановить индексацию, когда она не запущена");
            return false;
        }

        log.info("Остановка процесса индексации");
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
        }
        isIndexing.set(false);

        siteRepository.findAllByStatus(Status.INDEXING).forEach(site -> {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });
        return true;
    }

    @Override
    public boolean isIndexing() {
        return isIndexing.get();
    }

    @Override
    public boolean indexPage(String url) {
        log.warn("Метод indexPage в данный момент не поддерживается. Используйте полную индексацию.");
        return false;
    }
}