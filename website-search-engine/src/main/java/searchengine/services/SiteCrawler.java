package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
@Slf4j
public class SiteCrawler extends RecursiveAction {

    private final Site site;
    private final String url;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    private final Set<String> visitedUrls;

    // Нормализованный базовый URL сайта для сравнений
    private final String normalizedSiteUrl;

    public SiteCrawler(Site site, String url, IndexingService indexingService, SiteRepository siteRepository, PageRepository pageRepository, LemmaService lemmaService, Set<String> visitedUrls) {
        this.site = site;
        this.url = url;
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaService = lemmaService;
        this.visitedUrls = visitedUrls;
        this.normalizedSiteUrl = normalizeUrl(site.getUrl());
    }

    @Override
    protected void compute() {
        if (Thread.currentThread().isInterrupted() || !indexingService.isIndexing()) {
            log.debug("Индексация остановлена, прекращаем обход.");
            return;
        }

        String normalizedCurrentUrl = normalizeUrl(url);
        log.debug("Начинаем обход страницы: {}", normalizedCurrentUrl);

        if (visitedUrls.contains(normalizedCurrentUrl)) {
            log.debug("Страница уже посещена или находится в процессе обхода: {}", normalizedCurrentUrl);
            return;
        }

        visitedUrls.add(normalizedCurrentUrl);

        try {
            Thread.sleep(150);

            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Обход страницы " + normalizedCurrentUrl + " прерван перед сетевым запросом.");
            }

            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();
            String content = response.body();

            String path = normalizedCurrentUrl.substring(normalizedSiteUrl.length());
            if (path.isEmpty()) {
                path = "/";
            }

            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(statusCode);
            page.setContent(content);
            pageRepository.save(page);
            log.info("Страница сохранена: {} (Статус: {})", normalizedCurrentUrl, statusCode);

            if (statusCode < 400) {
                lemmaService.lemmatizePage(page);
                Document doc = response.parse();
                Elements links = doc.select("a[href]");
                Set<SiteCrawler> tasks = new HashSet<>();

                for (Element link : links) {
                    String absUrl = link.attr("abs:href");
                    String normalizedAbsUrl = normalizeUrl(absUrl);

                    if (isValidLink(normalizedAbsUrl)) {
                        tasks.add(new SiteCrawler(site, absUrl, indexingService, siteRepository, pageRepository, lemmaService, visitedUrls));
                    }
                }
                if (!tasks.isEmpty()) {
                    invokeAll(tasks);
                }
            }

        } catch (IOException e) {
            handleError("Ошибка ввода-вывода при обходе страницы " + normalizedCurrentUrl + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Обход страницы " + normalizedCurrentUrl + " прерван.");
        } catch (CancellationException e) {
            // ИСПРАВЛЕНО: Ловим CancellationException, чтобы он не попадал в общий Exception
            Thread.currentThread().interrupt();
            log.warn("Обход страницы " + normalizedCurrentUrl + " отменен (CancellationException).");
        } catch (Exception e) {
            log.error("Неизвестная ошибка при обходе страницы " + normalizedCurrentUrl, e);
            handleError("Неизвестная ошибка: " + e.getMessage());
        }
    }

    private boolean isValidLink(String link) {
        return link.startsWith(normalizedSiteUrl) &&
                !link.contains("#") &&
                !visitedUrls.contains(link) &&
                !link.matches(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|zip|rar|exe|mp3|mp4|avi|ppt|pptx)$");
    }

    private void handleError(String errorMessage) {
        if (indexingService.isIndexing()) {
            log.error(errorMessage);
            site.setLastError(errorMessage);
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
        }
    }

    private String normalizeUrl(String inputUrl) {
        if (inputUrl == null) return "";
        String normalized = inputUrl.toLowerCase();
        normalized = normalized.replace("://www.", "://");
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
