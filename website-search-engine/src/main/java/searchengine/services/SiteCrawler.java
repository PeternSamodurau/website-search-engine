package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.CrawlerConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Slf4j
public class SiteCrawler extends RecursiveAction {

    private final Site site;
    private final String url;
    private final CrawlerConfig crawlerConfig;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    private final AtomicBoolean isIndexing;
    private final Set<String> visitedUrls;

    @Override
    protected void compute() {
        String normalizedUrl = normalizeUrl(url);
        log.info("Начинаю обработку: {}", normalizedUrl);

        if (!isIndexing.get()) {
            log.warn("Индексация остановлена. Прерываю задачу для {}.", normalizedUrl);
            return;
        }
        if (!this.visitedUrls.add(normalizedUrl)) { // Изменено: используем this.visitedUrls
            log.warn("Уже посещено: {}. Пропускаю.", normalizedUrl);
            return;
        }

        try {
            Thread.sleep(crawlerConfig.getDelay()); // Используем задержку из конфига
            String path = new URL(url).getPath();

            if (pageRepository.findByPathAndSite(path, site).isPresent()) {
                log.debug("Страница {} уже существует в базе. Пропускаем.", path);
                return;
            }

            Connection.Response response = Jsoup.connect(url)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .timeout(crawlerConfig.getTimeout()) // Добавлено: используем таймаут из конфига
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

            if (statusCode >= 200 && statusCode < 300) {
                lemmaService.lemmatizePage(page);

                List<SiteCrawler> tasks = new ArrayList<>();
                log.debug("Ищу ссылки на странице {}", normalizedUrl);
                document.select("a[href]").stream()
                        .map(link -> link.absUrl("href"))
                        .forEach(link -> {
                            if (isLinkValid(link)) {
                                log.info("Найдена валидная ссылка: {} -> {}. Создаю подзадачу.", normalizedUrl, link);
                                // Изменено: передаем this.visitedUrls дочерней задаче
                                SiteCrawler task = new SiteCrawler(site, link, crawlerConfig, pageRepository, lemmaService, isIndexing, this.visitedUrls);
                                tasks.add(task);
                                task.fork();
                            } else {
                                log.debug("Ссылка {} отброшена как невалидная.", link);
                            }
                        });

                for (SiteCrawler task : tasks) {
                    task.join();
                }
            }

        } catch (Exception e) {
            log.error("Ошибка при обработке URL: {}. Ошибка: {}", url, e.getMessage());
        }
    }

    private boolean isLinkValid(String link) {
        boolean isEmpty = link.isEmpty();

        String normalizedLink = link.replaceFirst("://www\\.", "://");
        String normalizedSiteUrl = site.getUrl().replaceFirst("://www\\.", "://");
        boolean startsWithSite = normalizedLink.startsWith(normalizedSiteUrl);

        boolean isVisited = this.visitedUrls.contains(normalizeUrl(link)); // Изменено: используем this.visitedUrls
        boolean hasAnchor = link.contains("#");
        boolean isFile = link.matches(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|exe|mp3|mp4|avi|mov)$");

        if (isEmpty || !startsWithSite || isVisited || hasAnchor || isFile) {
            log.trace("Проверка ссылки {}: isEmpty={}, startsWithSite={}, isVisited={}, hasAnchor={}, isFile={}",
                    link, isEmpty, startsWithSite, isVisited, hasAnchor, isFile);
            return false;
        }
        return true;
    }

    private String normalizeUrl(String urlToNormalize) {
        if (urlToNormalize != null && urlToNormalize.endsWith("/")) {
            return urlToNormalize.substring(0, urlToNormalize.length() - 1);
        }
        return urlToNormalize;
    }
}
