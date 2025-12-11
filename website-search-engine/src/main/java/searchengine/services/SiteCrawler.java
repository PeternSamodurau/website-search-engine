package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.CrawlerConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
public class SiteCrawler extends RecursiveAction {

    private final Site site;
    private final String url;
    private final CrawlerConfig crawlerConfig;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaService lemmaService;
    private final Supplier<Boolean> isIndexing;
    private final Set<String> visitedUrls;

    @Override
    protected void compute() {
        String normalizedUrl = normalizeUrl(url);
        log.info("Начинаю обработку: {}", normalizedUrl);

        if (!isIndexing.get()) {
            log.warn("Индексация остановлена. Прерываю задачу для {}.", normalizedUrl);
            return;
        }
        if (!this.visitedUrls.add(normalizedUrl)) {
            log.warn("Уже посещено: {}. Пропускаю.", normalizedUrl);
            return;
        }

        try {
            // Используем настраиваемую задержку из конфигурации
            int minDelay = crawlerConfig.getMinDelay();
            int maxDelay = crawlerConfig.getMaxDelay();
            long randomDelay = (minDelay >= maxDelay) ? minDelay : ThreadLocalRandom.current().nextLong(minDelay, maxDelay + 1);

            log.debug("Задержка перед запросом: {} мс", randomDelay);
            Thread.sleep(randomDelay);

            String path = new URL(url).getPath();

            if (pageRepository.findByPathAndSite(path, site).isPresent()) {
                log.debug("Страница {} уже существует в базе. Пропускаем.", path);
                return;
            }

            Connection.Response response = Jsoup.connect(url)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .timeout(crawlerConfig.getTimeout())
                    .execute();

            int statusCode = response.statusCode();
            Document document = response.parse();
            String content = document.outerHtml();

            Page page = new Page();
            page.setSite(site);
            page.setPath(path.isEmpty() ? "/" : path);
            page.setCode(statusCode);
            page.setContent(content);
            pageRepository.save(page);
            log.info("Сохранена страница: {} (Код: {})", normalizedUrl, statusCode);

            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            if (statusCode >= 200 && statusCode < 300) {
                lemmaService.lemmatizePage(page);

                List<SiteCrawler> tasks = new ArrayList<>();
                log.debug("Ищу ссылки на странице {}", normalizedUrl);
                document.select("a[href]").stream()
                        .map(link -> link.absUrl("href"))
                        .forEach(link -> {
                            if (isLinkValid(link)) {
                                log.info("Найдена валидная ссылка: {} -> {}. Создаю подзадачу.", normalizedUrl, link);
                                SiteCrawler task = new SiteCrawler(site, link, crawlerConfig, pageRepository, siteRepository, lemmaService, isIndexing, this.visitedUrls);
                                tasks.add(task);
                                task.fork();
                            }
                        });

                for (SiteCrawler task : tasks) {
                    task.join();
                }
            } else {
                log.warn("Страница {} получила код состояния {}, поэтому не будет проиндексирована и просканирована на наличие ссылок.", normalizedUrl, statusCode);
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке URL: {}. Ошибка: {}", url, e.getMessage());
            site.setStatus(Status.FAILED);
            site.setLastError("Ошибка при обработке URL: " + url + ". " + e.getMessage());
            siteRepository.save(site);
        }
    }

    private boolean isLinkValid(String link) {
        if (link.isEmpty()) {
            log.debug("Ссылка {} отброшена: пустая.", link);
            return false;
        }

        // Проверки на оригинальной ссылке
        if (link.contains("#")) {
            log.debug("Ссылка {} отброшена: содержит якорь.", link);
            return false;
        }
        if (link.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|exe|mp3|mp4|avi|mov)$")) {
            log.debug("Ссылка {} отброшена: является файлом.", link);
            return false;
        }

        // Использование полностью нормализованной ссылки для остальных проверок
        String normalizedLink = normalizeUrl(link);

        String normalizedSiteUrl = normalizeUrl(site.getUrl());
        if (!normalizedLink.startsWith(normalizedSiteUrl)) {
            log.debug("Ссылка {} (нормализованная: {}) отброшена: не принадлежит текущему сайту (нормализованный: {}).", link, normalizedLink, normalizedSiteUrl);
            return false;
        }

        if (this.visitedUrls.contains(normalizedLink)) {
            log.debug("Ссылка {} (нормализованная: {}) отброшена: уже посещена.", link, normalizedLink);
            return false;
        }

        return true;
    }

    private String normalizeUrl(String urlToNormalize) {
        if (urlToNormalize == null || urlToNormalize.isEmpty()) {
            return urlToNormalize;
        }

        String normalized = urlToNormalize.toLowerCase();

        // Удаление query-параметров и якорей
        int queryIndex = normalized.indexOf('?');
        if (queryIndex != -1) {
            normalized = normalized.substring(0, queryIndex);
        }
        int anchorIndex = normalized.indexOf('#');
        if (anchorIndex != -1) {
            normalized = normalized.substring(0, anchorIndex);
        }

        // Удаление "www."
        normalized = normalized.replaceFirst("://www\\.", "://");

        // Удаление конечного слэша
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }
}