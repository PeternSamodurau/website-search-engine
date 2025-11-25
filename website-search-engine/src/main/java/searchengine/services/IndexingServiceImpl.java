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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    public static final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private ExecutorService siteExecutor;
    private ForkJoinPool pageCrawlerPool;


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

            siteExecutor = Executors.newFixedThreadPool(Math.min(sites.getSites().size(), 4));

            new Thread(() -> {
                try {
                    List<CompletableFuture<Void>> futures = sites.getSites().stream()
                            .map(siteConfig -> CompletableFuture.runAsync(() -> {
                                if (!isIndexing.get()) return;
                                indexSite(siteConfig);
                            }, siteExecutor))
                            .collect(Collectors.toList());

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                } catch (Exception e) {
                    log.error("Критическая ошибка в главном потоке индексации", e);
                } finally {
                    isIndexing.set(false);
                    if (siteExecutor != null && !siteExecutor.isShutdown()) {
                        siteExecutor.shutdown();
                    }
                    log.info("Процесс индексации ВСЕХ сайтов завершен.");
                }
            }, "Indexing-Manager-Thread").start();
            return true;
        } else {
            log.warn("Попытка запуска индексации, когда она уже запущена");
            return false;
        }
    }

    private void indexSite(SiteConfig siteConfig) {
        if (!isIndexing.get()) {
            log.info("Индексация остановлена пользователем. Пропускаем сайт {}.", siteConfig.getName());
            return;
        }

        Site site = siteRepository.findByUrl(siteConfig.getUrl())
                .orElseGet(() -> {
                    Site newSite = new Site();
                    newSite.setUrl(siteConfig.getUrl());
                    newSite.setName(siteConfig.getName());
                    return newSite;
                });

        if (site.getId() > 0) {
            log.info("Очистка старых данных для сайта: {}", site.getName());
            List<Page> pagesToDelete = pageRepository.findBySite(site);
            if (pagesToDelete != null && !pagesToDelete.isEmpty()) {
                pagesToDelete.forEach(indexRepository::deleteByPage);
            }
            pageRepository.deleteAllBySite(site);
            lemmaRepository.deleteAllBySite(site);
        }

        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(null);
        siteRepository.save(site);

        log.info("Запуск индексации для сайта: {}", site.getName());

        pageCrawlerPool = new ForkJoinPool();
        try {
            SiteCrawler.init();
            SiteCrawler task = new SiteCrawler(site, site.getUrl(), crawlerConfig, pageRepository, lemmaService, siteRepository);
            pageCrawlerPool.invoke(task);

            Site updatedSite = siteRepository.findById(site.getId()).orElse(null);

            if (updatedSite == null) {
                log.warn("Сайт {} был удален во время индексации, обновление статуса невозможно.", site.getName());
                return;
            }

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
        } finally {
            if (pageCrawlerPool != null && !pageCrawlerPool.isShutdown()) {
                pageCrawlerPool.shutdown();
            }
        }
    }


    @Override
    public boolean stopIndexing() {
        if (!isIndexing.get()) {
            log.warn("Попытка остановить индексацию, когда она не запущена");
            return false;
        }
        log.info("Остановка процесса индексации...");
        isIndexing.set(false);

        if (siteExecutor != null && !siteExecutor.isShutdown()) {
            siteExecutor.shutdownNow();
        }
        if (pageCrawlerPool != null && !pageCrawlerPool.isShutdown()) {
            pageCrawlerPool.shutdownNow();
        }

        siteRepository.findAllByStatus(Status.INDEXING).forEach(site -> {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем.");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        });
        return true;
    }

    @Override
    @Transactional
    public boolean indexPage(String url) {
        log.info("Запрос на индексацию отдельной страницы: {}", url);

        if (isIndexing.get()) {
            log.error("Индексация уже запущена. Невозможно проиндексировать отдельную страницу.");
            return false;
        }

        SiteConfig siteConfig = sites.getSites().stream()
                .filter(s -> url.startsWith(s.getUrl()))
                .findFirst()
                .orElse(null);

        if (siteConfig == null) {
            log.error("Страница {} не принадлежит ни одному сайту из конфигурации.", url);
            return false;
        }

        try {
            Site site = siteRepository.findByUrl(siteConfig.getUrl())
                    .orElseGet(() -> {
                        log.info("Сайт {} не найден в БД, создаю новую запись.", siteConfig.getName());
                        Site newSite = new Site();
                        newSite.setUrl(siteConfig.getUrl());
                        newSite.setName(siteConfig.getName());
                        newSite.setStatus(Status.INDEXING);
                        newSite.setStatusTime(LocalDateTime.now());
                        return siteRepository.save(newSite);
                    });

            String path = url.substring(siteConfig.getUrl().length());
            if (path.isEmpty()) {
                path = "/";
            }
            
            final String finalPath = path;
            pageRepository.findByPathAndSite(finalPath, site).ifPresent(pageToDelete -> {
                log.warn("Обнаружена существующая страница {}. Запускается упрощенная процедура удаления.", finalPath);
                indexRepository.deleteByPage(pageToDelete);
                pageRepository.delete(pageToDelete);
            });

            log.info("Начинаю индексацию страницы: {}", url);
            org.jsoup.Connection.Response response = org.jsoup.Jsoup.connect(url)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .execute();

            Page newPage = new Page();
            newPage.setSite(site);
            newPage.setPath(path);
            newPage.setCode(response.statusCode());
            newPage.setContent(response.body());
            pageRepository.save(newPage);

            if (response.statusCode() < 400) {
                lemmaService.lemmatizePage(newPage);
            }

            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            log.info("Индексация страницы {} успешно завершена.", url);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при индексации страницы {}: {}", url, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isIndexing() {
        return isIndexing.get();
    }
}
