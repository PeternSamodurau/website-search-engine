package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
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

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;
    private Thread indexingThread;

    @Override
    public boolean startIndexing() {
        if (isIndexing.compareAndSet(false, true)) {
            log.info("Запуск процесса индексации");

            forkJoinPool = new ForkJoinPool();

            indexingThread = new Thread(() -> {
                try {
                    siteRepository.findAll().forEach(site -> {
                        if (site.getStatus() == Status.INDEXING) {
                            site.setStatus(Status.FAILED);
                            site.setLastError("Индексация была прервана");
                            siteRepository.save(site);
                        }
                    });

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
                        SiteCrawler task = new SiteCrawler(site, site.getUrl(), this, siteRepository, pageRepository, lemmaService, ConcurrentHashMap.newKeySet());
                        forkJoinPool.invoke(task);

                        Site updatedSite = siteRepository.findById(site.getId()).orElseThrow();
                        if (updatedSite.getStatus() != Status.FAILED) {
                            updatedSite.setStatus(Status.INDEXED);
                            updatedSite.setStatusTime(LocalDateTime.now());
                            siteRepository.save(updatedSite);
                            log.info("Индексация сайта '{}' завершена.", updatedSite.getName());
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof CancellationException) {
                        log.info("Процесс индексации был прерван пользователем.");
                    } else {
                        log.error("Критическая ошибка во время процесса индексации", e);
                        siteRepository.findAllByStatus(Status.INDEXING).forEach(s -> {
                            s.setStatus(Status.FAILED);
                            s.setLastError("Критическая ошибка во время индексации: " + e.getMessage());
                            s.setStatusTime(LocalDateTime.now());
                            siteRepository.save(s);
                        });
                    }
                } finally {
                    if (forkJoinPool != null) {
                        forkJoinPool.shutdown();
                    }
                    isIndexing.set(false);
                    log.info("Процесс индексации ВСЕХ сайтов завершен.");
                }
            }, "Indexing-Manager-Thread");
            indexingThread.start();
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
        isIndexing.set(false);
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
        }
        if (indexingThread != null && indexingThread.isAlive()) {
            indexingThread.interrupt();
        }

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
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("HeliontSearchBot/1.0")
                    .referrer("http://www.google.com")
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

    @Transactional
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
        return url.replace("://www.", "://");
    }
}