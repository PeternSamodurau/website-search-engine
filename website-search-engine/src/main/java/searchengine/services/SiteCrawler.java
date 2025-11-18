package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.CrawlerConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final CrawlerConfig crawlerConfig;

    @Override
    protected void compute() {
        if (!indexingService.isIndexing() || Thread.currentThread().isInterrupted()) {
            return;
        }

        if (!visitedUrls.add(url)) {
            return;
        }

        log.debug("Обход страницы: {}", url);

        try {
            Thread.sleep(crawlerConfig.getDelay());

            Connection.Response response = Jsoup.connect(url)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();
            String content = (response.contentType() != null && response.contentType().startsWith("text/html"))
                    ? response.body() : "";

            String path = url.replaceFirst(site.getUrl(), "");
            if (path.isEmpty()) {
                path = "/";
            }

            Page page = new Page();
            page.setSite(site);
            page.setPath(path);
            page.setCode(statusCode);
            page.setContent(content);
            pageRepository.save(page);
            log.info("Сохранена страница: {} (Статус: {})", url, statusCode);

            if (statusCode < 400 && !content.isEmpty()) {
                lemmaService.lemmatizePage(page);

                Document doc = response.parse();
                Elements links = doc.select("a[href]");
                List<SiteCrawler> tasks = new ArrayList<>();

                for (Element link : links) {
                    String absUrl = link.attr("abs:href");
                    if (isValidLink(absUrl)) {

                        tasks.add(new SiteCrawler(site, absUrl, indexingService, siteRepository, pageRepository, lemmaService, visitedUrls, crawlerConfig));
                    }
                }

                if (!tasks.isEmpty()) {
                    invokeAll(tasks);
                }
            }

        } catch (InterruptedException | CancellationException e) {
            Thread.currentThread().interrupt();
            log.warn("Обход страницы {} прерван.", url);
        } catch (Exception e) {

            handleError("Ошибка при обходе страницы " + url + ": " + e.getMessage());
        }
    }

    private boolean isValidLink(String link) {
        if (link == null || link.isBlank()) {
            return false;
        }
        String normalizedLink = normalizeUrl(link);
        if (!normalizedLink.startsWith(normalizeUrl(site.getUrl()))) {
            return false;
        }
        if (normalizedLink.matches(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|zip|rar|exe|mp3|mp4|avi|ppt|pptx)$")) {
            return false;
        }
        return !normalizedLink.contains("#");
    }

    private String normalizeUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }


    private void handleError(String errorMessage) {
        log.error(errorMessage);
        siteRepository.findById(site.getId()).ifPresent(s -> {
            s.setStatus(Status.FAILED);
            s.setLastError(errorMessage);
            s.setStatusTime(LocalDateTime.now());
            siteRepository.save(s);
        });
    }
}