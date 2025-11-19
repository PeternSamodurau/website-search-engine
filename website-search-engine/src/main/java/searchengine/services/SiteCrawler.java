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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
@Slf4j
public class SiteCrawler extends RecursiveAction {

    private static volatile Set<String> visitedUrls;

    private final Site site;
    private final String url;
    private final CrawlerConfig crawlerConfig;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;

    public static void init() {
        visitedUrls = ConcurrentHashMap.newKeySet();
    }

    @Override
    protected void compute() {
        if (visitedUrls.contains(url) || !IndexingServiceImpl.isIndexing.get()) {
            return;
        }
        visitedUrls.add(url);

        try {
            Thread.sleep(150);
            String path = new URL(url).getPath();

            if (pageRepository.findByPathAndSite(path, site).isPresent()) {
                log.debug("Страница {} уже существует в базе. Пропускаем.", path);
                return;
            }

            Connection.Response response = Jsoup.connect(url)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
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

            if (statusCode >= 200 && statusCode < 300) {
                lemmaService.lemmatizePage(page);

                List<SiteCrawler> tasks = new ArrayList<>();
                document.select("a[href]").stream()
                        .map(link -> link.absUrl("href"))
                        .filter(this::isLinkValid)
                        .forEach(link -> {
                            SiteCrawler task = new SiteCrawler(site, link, crawlerConfig, pageRepository, lemmaService);
                            tasks.add(task);
                            task.fork();
                        });

                for (SiteCrawler task : tasks) {
                    task.join();
                }
            }

        } catch (Exception e) {
            log.error("Error parsing URL: {} or thread was interrupted. Error: {}", url, e.getMessage());
        }
    }

    private boolean isLinkValid(String link) {
        return !link.isEmpty() &&
                link.startsWith(site.getUrl()) &&
                !link.equals(site.getUrl()) &&
                !visitedUrls.contains(link) &&
                !link.contains("#") &&
                !link.matches(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|exe|mp3|mp4|avi|mov)$");
    }
}
