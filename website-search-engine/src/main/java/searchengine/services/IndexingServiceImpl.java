package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.component.SiteDataCleaner;
import searchengine.config.CrawlerConfig;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private ExecutorService siteExecutor;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    private final SitesListConfig sites;
    private final CrawlerConfig crawlerConfig;
    private final SiteDataCleaner siteDataCleaner;

    @Override
    public boolean startIndexing() {
        if (isIndexing.compareAndSet(false, true)) {
            log.info("Запуск процесса индексации");

            siteExecutor = Executors.newFixedThreadPool(Math.min(sites.getSites().size(), 4));

            new Thread(() -> {
                try {
                    List<CompletableFuture<Void>> futures = sites.getSites().stream()
                            .map(siteConfig -> CompletableFuture.runAsync(() -> {
                                if (!isIndexing.get()) {
                                    log.info("Глобальная остановка индексации. Пропускаем сайт {}", siteConfig.getName());
                                    return;
                                }

                                // --- START: Исправленная логика для избежания гонки транзакций ---

                                // 1. Сначала полностью удаляем старый сайт и все его данные, если он существует.
                                // Это гарантирует, что мы начнем с чистого листа.
                                siteRepository.findByUrl(siteConfig.getUrl()).ifPresent(siteDataCleaner::clearDataForSite);

                                // 2. Создаем НОВЫЙ, чистый объект Site, так как старый был удален.
                                Site site = new Site();
                                site.setName(siteConfig.getName());
                                site.setUrl(siteConfig.getUrl());
                                site.setStatus(Status.INDEXING); // Сразу задаем начальный статус
                                site.setStatusTime(LocalDateTime.now());
                                site.setLastError(null);

                                // 3. Сохраняем новый сайт. Теперь `site` - это управляемый (managed) объект.
                                site = siteRepository.save(site);

                                // --- END: Исправленная логика ---

                                if (!siteConfig.getEnabled()) {
                                    site.setStatus(Status.FAILED);
                                    site.setLastError("Индексация пропущена: сайт отключен в конфигурации.");
                                    site.setStatusTime(LocalDateTime.now());
                                    siteRepository.save(site);
                                    log.info("Сайт '{}' пропущен (отключен в конфигурации). Статус: FAILED", siteConfig.getName());
                                    return; // Skip crawling for disabled sites
                                }

                                // Check site availability
                                String availabilityError = checkSiteAvailability(siteConfig.getUrl());
                                if (availabilityError != null) {
                                    site.setStatus(Status.FAILED);
                                    site.setLastError(availabilityError);
                                    site.setStatusTime(LocalDateTime.now());
                                    siteRepository.save(site);
                                    log.warn("Сайт '{}' недоступен. Статус: FAILED. Причина: {}", siteConfig.getName(), availabilityError);
                                    return; // Skip crawling for unavailable sites
                                }

                                // If enabled and available, proceed with actual crawling
                                indexSite(site, siteConfig);
                            }, siteExecutor))
                            .collect(Collectors.toList());

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                } catch (Exception e) {
                    log.error("Критическая ошибка в главном потоке индексации", e);
                } finally {
                    if (siteExecutor != null && !siteExecutor.isShutdown()) {
                        siteExecutor.shutdown();
                        try {
                            if (!siteExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                                log.warn("Не все задачи siteExecutor завершились после обычного завершения.");
                                siteExecutor.shutdownNow();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Ожидание завершения siteExecutor было прервано.", e);
                            siteExecutor.shutdownNow();
                        }
                    }
                    // This logic ensures isIndexing is set to false only if it wasn't stopped by stopIndexing()
                    if (isIndexing.get()) { // If flag is still true, it means indexing completed naturally
                        isIndexing.set(false);
                        log.info("Процесс индексации ВСЕХ сайтов завершен естественным путем.");
                    } else { // If flag is false, it means stopIndexing() was called
                        log.info("Процесс индексации ВСЕХ сайтов завершен (остановлен пользователем).");
                    }
                }
            }, "Indexing-Manager-Thread").start();
            return true;
        } else {
            log.warn("Попытка запуска индексации, когда она уже запущена");
            return false;
        }
    }

    private void indexSite(Site site, SiteConfig siteConfig) { // Modified signature
        if (!isIndexing.get()) {
            log.info("Обход сайта '{}' пропущен, так как индексация остановлена пользователем.", site.getName());
            // Update status to FAILED if it was INDEXING and not already FAILED
            if (site.getStatus() == Status.INDEXING) {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация остановлена пользователем");
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
            return;
        }

        log.info("Запуск обхода для сайта: {}", site.getName());

        ForkJoinPool pageCrawlerPool = new ForkJoinPool();
        Set<String> siteVisitedUrls = ConcurrentHashMap.newKeySet();

        try {
            SiteCrawler task = new SiteCrawler(site, site.getUrl(), crawlerConfig, pageRepository, lemmaService, this::isIndexing, siteVisitedUrls);
            pageCrawlerPool.invoke(task);

            Site updatedSite = siteRepository.findById(site.getId()).orElse(null);

            if (updatedSite == null) {
                log.warn("Сайт {} был удален во время обхода, обновление статуса невозможно.", site.getName());
                return;
            }

            if (isIndexing.get()) { // Check if indexing was not globally stopped
                updatedSite.setStatus(Status.INDEXED);
                log.info("Обход сайта '{}' успешно завершен.", updatedSite.getName());
            } else {
                // This branch means it was stopped by stopIndexing()
                updatedSite.setStatus(Status.FAILED);
                updatedSite.setLastError("Индексация остановлена пользователем");
                log.warn("Обход сайта '{}' был остановлен пользователем.", updatedSite.getName());
            }
            updatedSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(updatedSite);
        } catch (Exception e) {
            log.error("Ошибка при обходе сайта {}: {}", site.getName(), e.getMessage());
            Site updatedSite = siteRepository.findById(site.getId()).orElse(null);
            if (updatedSite != null) {
                updatedSite.setStatus(Status.FAILED);
                updatedSite.setLastError("Обход прерван: " + e.getMessage());
                updatedSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(updatedSite);
            }
        } finally {
            if (pageCrawlerPool != null && !pageCrawlerPool.isShutdown()) {
                if (!isIndexing.get()) { // If global stop flag is set, forcefully shut down
                    pageCrawlerPool.shutdownNow();
                } else {
                    pageCrawlerPool.shutdown(); // Otherwise, graceful shutdown
                }
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
        isIndexing.set(false); // Устанавливаем флаг остановки

        if (siteExecutor != null && !siteExecutor.isShutdown()) {
            siteExecutor.shutdownNow(); // Прерываем потоки, выполняющие indexSite
            try {
                // Ждем завершения всех задач после принудительной остановки
                if (!siteExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Не все задачи индексации завершились после принудительной остановки.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Ожидание завершения задач индексации было прервано.", e);
            }
        }
        
        // Обновляем статус всех сайтов, которые были в процессе индексации (INDEXING)
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
                lemmaService.deleteDataForPage(pageToDelete);
                pageRepository.delete(pageToDelete);
            });

            log.info("Начинаю индексацию страницы: {}", url);
            org.jsoup.Connection.Response response = org.jsoup.Jsoup.connect(url)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .timeout(crawlerConfig.getTimeout())
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

    private String checkSiteAvailability(String siteUrl) {
        try {
            Connection.Response response = Jsoup.connect(siteUrl)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .ignoreHttpErrors(true)
                    .timeout(crawlerConfig.getTimeout()) // Use crawlerConfig timeout
                    .execute();

            int responseCode = response.statusCode();
            if (responseCode >= 200 && responseCode < 400) {
                return null; // Site is available
            } else {
                return "Сайт вернул код ответа: " + responseCode + ". Тело ответа (первые 500 символов): " +
                       response.body().substring(0, Math.min(response.body().length(), 500));
            }
        } catch (IOException e) {
            return "Ошибка IO при проверке доступности сайта: " + e.getMessage();
        }
    }
}
