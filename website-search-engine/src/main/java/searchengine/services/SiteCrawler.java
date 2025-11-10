package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.CrawlerConfig;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

@Slf4j
public class SiteCrawler extends RecursiveAction {

    private final Site site;
    private final String path;
    private final Set<String> visitedLinks;
    private final PageRepository pageRepository;
    private final IndexingService indexingService;
    private final LemmaService lemmaService;
    private final CrawlerConfig crawlerConfig;

    public SiteCrawler(Site site, String path, Set<String> visitedLinks, PageRepository pageRepository, IndexingService indexingService, LemmaService lemmaService, CrawlerConfig crawlerConfig) {
        this.site = site;
        this.path = path;
        this.visitedLinks = visitedLinks;
        this.pageRepository = pageRepository;
        this.indexingService = indexingService;
        this.lemmaService = lemmaService;
        this.crawlerConfig = crawlerConfig;
    }

    @Override
    protected void compute() {
        if (!indexingService.isIndexing() || visitedLinks.contains(path)) {
            return;
        }
        visitedLinks.add(path);

        try {
            Thread.sleep(crawlerConfig.getDelay());
            Connection.Response response = Jsoup.connect(site.getUrl() + path)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .execute();

            int statusCode = response.statusCode();
            String content = response.body();

            // Синхронизируем сохранение, чтобы избежать проблем с многопоточностью
            synchronized (pageRepository) {
                Page page = new Page();
                page.setSite(site);
                page.setPath(path);
                page.setCode(statusCode);
                page.setContent(content);
                pageRepository.save(page);

                if (statusCode >= 200 && statusCode < 300) {
                    lemmaService.lemmatizePage(page);
                }
            }


            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Document doc = response.parse();
                Elements links = doc.select("a[href]");

                Set<SiteCrawler> tasks = links.stream()
                        .map(link -> link.attr("abs:href"))
                        .filter(this::isLinkValid)
                        .map(link -> new SiteCrawler(site, getRelativePath(link), visitedLinks, pageRepository, indexingService, lemmaService, crawlerConfig))
                        .collect(Collectors.toSet());

                invokeAll(tasks);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Ошибка при обработке страницы {}{}: {}", site.getUrl(), path, e.getMessage());
        }
    }

    private boolean isLinkValid(String link) {
        String rootUrl = site.getUrl();
        // Убираем www. для более надежного сравнения
        String simplifiedRoot = rootUrl.startsWith("https://www.") ? rootUrl.replace("https://www.", "https://") : rootUrl;
        simplifiedRoot = simplifiedRoot.startsWith("http://www.") ? simplifiedRoot.replace("http://www.", "http://") : simplifiedRoot;

        return !link.isEmpty() &&
                link.startsWith(simplifiedRoot) &&
                !link.contains("#") &&
                !link.matches(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|zip|rar|exe)$");
    }

    private String getRelativePath(String absolutePath) {
        return absolutePath.substring(site.getUrl().length() - 1);
    }
}
