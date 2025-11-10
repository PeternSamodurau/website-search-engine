package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status; // Импортируем Status
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;
    private final SitesListConfig sitesListConfig;

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private final List<ForkJoinPool> activeForkJoinPools = new CopyOnWriteArrayList<>();

    @Override
    public boolean startIndexing() {
        if (!isIndexing.compareAndSet(false, true)) {
            return false; // Индексация уже запущена
        }

        activeForkJoinPools.clear(); // Очищаем список пулов перед новой индексацией

        List<SiteConfig> sites = sitesListConfig.getSites();
        for (SiteConfig siteConfig : sites) {
            Site site = new Site();
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            ForkJoinPool forkJoinPool = new ForkJoinPool();
            activeForkJoinPools.add(forkJoinPool); // Добавляем пул в список
            SiteIndexer siteIndexer = new SiteIndexer(site, siteRepository, this); // Передаем this
            forkJoinPool.execute(siteIndexer); // Асинхронный запуск
        }
        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (!isIndexing.compareAndSet(true, false)) {
            return false; // Индексация не запущена
        }

        for (ForkJoinPool pool : activeForkJoinPools) {
            pool.shutdownNow(); // Попытка немедленно остановить все задачи
        }
        activeForkJoinPools.clear(); // Очищаем список пулов
        log.info("Индексация остановлена.");
        return true;
    }

    @Override
    public boolean isIndexing() {
        return isIndexing.get();
    }

    @Override
    @Transactional
    public boolean indexPage(String url) {
        if (!isIndexing.get()) {
            log.warn("Индексация остановлена, страница {} не будет проиндексирована.", url);
            return false;
        }

        // 1. Проверка, принадлежит ли URL одному из сайтов в конфигурации
        SiteConfig matchingSiteConfig = null;
        for (SiteConfig siteConfig : sitesListConfig.getSites()) {
            if (url.startsWith(siteConfig.getUrl())) {
                matchingSiteConfig = siteConfig;
                break;
            }
        }

        if (matchingSiteConfig == null) {
            log.warn("Страница {} находится за пределами сайтов, указанных в конфигурационном файле.", url);
            return false;
        }

        // Создаем final переменную для использования в лямбда-выражении
        final SiteConfig finalMatchingSiteConfig = matchingSiteConfig;

        // Получаем сущность Site из базы данных
        Site site = siteRepository.findByUrl(finalMatchingSiteConfig.getUrl())
                .orElseGet(() -> {
                    // Если сайт не найден в базе, создаем его (это может произойти, если startIndexing не запускался)
                    Site newSite = new Site();
                    newSite.setName(finalMatchingSiteConfig.getName());
                    newSite.setUrl(finalMatchingSiteConfig.getUrl());
                    newSite.setStatus(Status.INDEXING);
                    newSite.setStatusTime(LocalDateTime.now());
                    return siteRepository.save(newSite);
                });

        // 2. Если страница уже была проиндексирована, удалить всю старую информацию
        String path = url.substring(site.getUrl().length());
        if (path.isEmpty()) {
            path = "/"; // Главная страница
        }

        Optional<Page> existingPageOptional = pageRepository.findByPathAndSite(path, site);

        if (existingPageOptional.isPresent()) {
            Page existingPage = existingPageOptional.get();
            log.info("Обновление индекса для существующей страницы: {}", url);

            // Удаляем старые индексы и обновляем частоту лемм
            List<Index> oldIndexes = indexRepository.findByPage(existingPage);
            for (Index oldIndex : oldIndexes) {
                Lemma lemma = oldIndex.getLemma();
                lemma.setFrequency(lemma.getFrequency() - 1);
                if (lemma.getFrequency() <= 0) {
                    lemmaRepository.delete(lemma);
                } else {
                    lemmaRepository.save(lemma);
                }
            }
            indexRepository.deleteAll(oldIndexes);
            pageRepository.delete(existingPage);
        } else {
            log.info("Индексация новой страницы: {}", url);
        }

        // 3. Получить HTML-код страницы
        Document doc;
        int statusCode;
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .execute();
            statusCode = response.statusCode();

            // --- ДОБАВЛЕННАЯ ПРОВЕРКА CONTENT-TYPE ---
            String contentType = response.contentType();
            if (contentType == null || (!contentType.startsWith("text/") && !contentType.contains("xml"))) {
                log.warn("Страница {} имеет необрабатываемый тип контента: {}. Пропускаем индексацию.", url, contentType);
                return false;
            }
            // --- КОНЕЦ ДОБАВЛЕННОЙ ПРОВЕРКИ ---

            doc = response.parse();
        } catch (IOException e) {
            log.error("Ошибка при получении HTML страницы {}: {}", url, e.getMessage());
            return false;
        }

        if (statusCode >= 400) {
            log.warn("Страница {} вернула статус-код {}. Не будет проиндексирована.", url, statusCode);
            Page errorPage = new Page();
            errorPage.setSite(site);
            errorPage.setPath(path);
            errorPage.setCode(statusCode);
            errorPage.setContent(""); // Содержимое пустое для страниц с ошибками
            pageRepository.save(errorPage);
            return false;
        }

        // 4. Сохранить новую запись в таблицу page
        Page newPage = new Page();
        newPage.setSite(site);
        newPage.setPath(path);
        newPage.setCode(statusCode);
        newPage.setContent(doc.html());
        pageRepository.save(newPage);

        // 5. Использовать алгоритм лемматизации
        String textContent = doc.body().text();
        HashMap<String, Integer> lemmasMap = lemmaService.collectLemmas(textContent);

        // 6. Для каждой леммы из HashMap
        for (Map.Entry<String, Integer> entry : lemmasMap.entrySet()) {
            String lemmaText = entry.getKey();
            Integer rank = entry.getValue();

            Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaText, site)
                    .orElseGet(() -> {
                        Lemma newLemma = new Lemma();
                        newLemma.setLemma(lemmaText);
                        newLemma.setSite(site);
                        newLemma.setFrequency(0); // Частота будет обновлена ниже
                        return newLemma;
                    });

            lemma.setFrequency(lemma.getFrequency() + 1);
            lemmaRepository.save(lemma);

            // Создать запись в таблице index
            Index index = new Index();
            index.setPage(newPage);
            index.setLemma(lemma);
            index.setRank(rank);
            indexRepository.save(index);
        }

        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site); // Обновляем время индексации сайта

        return true;
    }

    private static class SiteIndexer extends java.util.concurrent.RecursiveAction {
        private final Site site;
        private final SiteRepository siteRepository;
        private final IndexingService indexingService; // Добавлено для вызова indexPage

        private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
        private final ConcurrentLinkedQueue<String> urlsToVisit = new ConcurrentLinkedQueue<>();

        public SiteIndexer(Site site, SiteRepository siteRepository,
                           IndexingService indexingService) {
            this.site = site;
            this.siteRepository = siteRepository;
            this.indexingService = indexingService;
        }

        @Override
        protected void compute() {
            try {
                site.setStatus(Status.INDEXING);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                String rootUrl = site.getUrl();
                urlsToVisit.add(rootUrl);
                visitedUrls.add(rootUrl); // Добавляем корневой URL в посещенные

                while (!urlsToVisit.isEmpty() && indexingService.isIndexing()) {
                    String currentUrl = urlsToVisit.poll();
                    if (currentUrl == null) continue;

                    // Вызываем indexPage для текущего URL
                    boolean indexedSuccessfully = indexingService.indexPage(currentUrl);

                    if (indexedSuccessfully) {
                        try {
                            Connection.Response response = Jsoup.connect(currentUrl)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                                    .referrer("http://www.google.com")
                                    .timeout(10000)
                                    .ignoreHttpErrors(true)
                                    .execute();
                            Document doc = response.parse();
                            Elements links = doc.select("a[href]");

                            for (org.jsoup.nodes.Element link : links) {
                                String absUrl = link.absUrl("href");
                                if (isValidUrl(absUrl, rootUrl) && !visitedUrls.contains(absUrl)) {
                                    visitedUrls.add(absUrl);
                                    urlsToVisit.add(absUrl);
                                }
                            }
                        } catch (IOException e) {
                            log.error("Ошибка при получении HTML для обхода ссылок {}: {}", currentUrl, e.getMessage());
                        }
                    }
                    // Обновляем время статуса сайта после каждой проиндексированной страницы
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }

                if (indexingService.isIndexing()) {
                    site.setStatus(Status.INDEXED);
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    log.info("Индексация сайта {} завершена.", site.getUrl());
                } else {
                    site.setStatus(Status.FAILED);
                    site.setLastError("Индексация остановлена пользователем.");
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    log.warn("Индексация сайта {} остановлена.", site.getUrl());
                }

            } catch (Exception e) {
                log.error("Ошибка при индексации сайта {}: {}", site.getUrl(), e.getMessage());
                site.setStatus(Status.FAILED);
                site.setLastError("Ошибка индексации: " + e.getMessage());
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        }

        private boolean isValidUrl(String url, String rootUrl) {
            // Проверяем, что ссылка относится к текущему сайту
            // Исключаем ссылки на файлы (pdf, doc, jpg и т.д.)
            // Исключаем якоря (#)
            return url.startsWith(rootUrl) &&
                    !url.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|gz|tar|tgz|jar|png|jpg|jpeg|gif|bmp|svg|mp3|mp4|avi|mov|wmv|flv|webm)$") &&
                    !url.contains("#");
        }
    }
}