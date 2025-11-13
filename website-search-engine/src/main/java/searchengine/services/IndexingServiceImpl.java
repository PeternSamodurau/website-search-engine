package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.config.CrawlerConfig; // ДОБАВЛЕНО
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    private final SitesListConfig sites;
    private final CrawlerConfig crawlerConfig; // ДОБАВЛЕНО

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;

    @Override
    public boolean startIndexing() {
        if (isIndexing.compareAndSet(false, true)) {
            log.info("Запуск процесса индексации");

            new Thread(() -> {
                try {
                    siteRepository.findAll().forEach(site -> {
                        if (site.getStatus() == Status.INDEXING) {
                            site.setStatus(Status.FAILED);
                            site.setLastError("Индексация была прервана");
                            siteRepository.save(site);
                        }
                    });

                    forkJoinPool = new ForkJoinPool();

                    for (SiteConfig siteConfig : sites.getSites()) {
                        if (!isIndexing.get()) {
                            log.info("Индексация остановлена пользователем. Пропускаем оставшиеся сайты.");
                            break;
                        }

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

                        log.info("Запуск индексации для сайта: {}", site.getName());
                        // ИЗМЕНЕНО: Добавлен crawlerConfig в конструктор
                        SiteCrawler task = new SiteCrawler(site, site.getUrl(), this, siteRepository, pageRepository, lemmaService, ConcurrentHashMap.newKeySet(), crawlerConfig);
                        forkJoinPool.invoke(task);

                        Site updatedSite = siteRepository.findById(site.getId()).orElseThrow();
                        if (isIndexing.get() && updatedSite.getStatus() != Status.FAILED) {
                            updatedSite.setStatus(Status.INDEXED);
                            updatedSite.setStatusTime(LocalDateTime.now());
                            siteRepository.save(updatedSite);
                            log.info("Индексация сайта '{}' успешно завершена.", updatedSite.getName());
                        } else {
                            log.warn("Индексация сайта '{}' была остановлена или завершилась с ошибкой.", updatedSite.getName());
                        }
                    }
                } catch (Exception e) {
                    log.error("Критическая ошибка в главном потоке индексации", e);
                } finally {
                    isIndexing.set(false);
                    if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
                        forkJoinPool.shutdown();
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

    @Override
    public boolean stopIndexing() {
        if (!isIndexing.get()) {
            log.warn("Попытка остановить индексацию, когда она не запущена");
            return false;
        }

        log.info("Остановка процесса индексации...");
        isIndexing.set(false);

        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
            try {
                if (!forkJoinPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("ForkJoinPool не завершился в течение 5 секунд.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        siteRepository.findAllByStatus(Status.INDEXING).forEach(site -> {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            log.info("Сайт '{}' помечен как остановленный.", site.getName());
        });

        log.info("Процесс индексации остановлен.");
        return true;
    }

    @Override
    public boolean isIndexing() {
        return isIndexing.get();
    }

    @Override
    public boolean indexPage(String url) {
        log.info("Запрос на индексацию отдельной страницы: {}", url);

        Optional<SiteConfig> optionalSiteConfig = sites.getSites().stream()
                .filter(s -> normalizeUrl(url).startsWith(normalizeUrl(s.getUrl())))
                .findFirst();

        if (optionalSiteConfig.isEmpty()) {
            log.error("Страница {} находится за пределами сайтов, указанных в конфигурационном файле.", url);
            return false;
        }

        SiteConfig siteConfig = optionalSiteConfig.get();
        Site site = siteRepository.findByUrl(siteConfig.getUrl())
                .orElseGet(() -> {
                    Site newSite = new Site();
                    newSite.setUrl(siteConfig.getUrl());
                    newSite.setName(siteConfig.getName());
                    newSite.setStatus(Status.INDEXED);
                    newSite.setStatusTime(LocalDateTime.now());
                    return siteRepository.save(newSite);
                });

        String path = url.substring(siteConfig.getUrl().length());
        if (path.isEmpty()) {
            path = "/";
        }

        pageRepository.findByPathAndSite(path, site).ifPresent(this::deletePageData);

        try {
            // ИЗМЕНЕНО: Используем значения из конфигурации
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .execute();

            int statusCode = response.statusCode();
            String content = response.body();

            Page newPage = new Page();
            newPage.setSite(site);
            newPage.setPath(path);
            newPage.setCode(statusCode);
            newPage.setContent(content);
            pageRepository.save(newPage);

            if (statusCode < 400) {
                lemmaService.lemmatizePage(newPage);
                log.info("Страница {} успешно проиндексирована.", url);
            } else {
                log.warn("Страница {} вернула код состояния {}. Контент сохранен, но не лемматизирован.", url, statusCode);
            }

            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            return true;

        } catch (IOException e) {
            log.error("Ошибка при индексации страницы " + url, e);
            site.setStatus(Status.FAILED);
            site.setLastError("Не удалось проиндексировать страницу: " + url + ". Ошибка: " + e.getMessage());
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            return false;
        }
    }

    @org.springframework.transaction.annotation.Transactional
    private void deletePageData(Page page) {
        log.info("Страница {} уже была проиндексирована. Удаление старых данных.", page.getPath());
        List<Index> indices = indexRepository.findByPage(page);

        List<Lemma> lemmasToUpdate = indices.stream()
                .map(Index::getLemma)
                .peek(lemma -> lemma.setFrequency(lemma.getFrequency() - 1))
                .collect(Collectors.toList());

        if (!lemmasToUpdate.isEmpty()) {
            lemmaRepository.saveAll(lemmasToUpdate);
        }
        indexRepository.deleteAll(indices);
        pageRepository.delete(page);
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        return url.toLowerCase().replace("://www.", "://");
    }
}