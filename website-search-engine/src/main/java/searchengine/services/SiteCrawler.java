package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
public class SiteCrawler extends RecursiveAction {
    private final Site site;
    private final String url;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    private final Set<String> visitedUrls; // <-- ИЗМЕНЕНИЕ: Убрали static, добавили в конструктор

    @Override
    protected void compute() {
        if (!indexingService.isIndexing() || visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);

        try {
            Thread.sleep(150); // Небольшая задержка
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                    .referrer("http://www.google.com")
                    .execute();

            int statusCode = response.statusCode();
            String content = response.body();

            Page page = new Page();
            page.setSite(site);
            page.setPath(url.replace(site.getUrl(), ""));
            page.setCode(statusCode);
            page.setContent(content);
            pageRepository.save(page);

            if (statusCode >= 200 && statusCode < 300) {
                lemmaService.lemmatizePage(page);

                Document doc = response.parse();
                Elements links = doc.select("a[href]");
                Set<SiteCrawler> tasks = new HashSet<>();

                for (org.jsoup.nodes.Element link : links) {
                    String newUrl = link.attr("abs:href");
                    if (newUrl.startsWith(site.getUrl()) && !newUrl.equals(url) && !visitedUrls.contains(newUrl) && !newUrl.contains("#")) {
                        tasks.add(new SiteCrawler(site, newUrl, indexingService, siteRepository, pageRepository, lemmaService, visitedUrls));
                    }
                }
                invokeAll(tasks);
            }
        } catch (Exception e) {
            // Игнорируем ошибки отдельных страниц, чтобы не останавливать всю индексацию
        }
    }
}