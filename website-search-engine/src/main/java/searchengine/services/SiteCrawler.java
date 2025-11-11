package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status; // Импортируем Status enum
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import javax.net.ssl.SSLHandshakeException; // Импортируем специфическое SSL исключение

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
                    .timeout(10000) // Добавлен таймаут 10 секунд
                    .execute();

            int statusCode = response.statusCode();
            String content = response.body();

            Page page = new Page();
            page.setSite(site);
            // ИСПРАВЛЕНО: Установка пути для корневого URL
            String relativePath = url.replace(site.getUrl(), "");
            if (relativePath.isEmpty()) {
                page.setPath("/"); // Для корневого URL устанавливаем путь в "/"
            } else {
                page.setPath(relativePath);
            }
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
            } else {
                // Логируем не-2xx статусы для дочерних страниц, но не помечаем весь сайт как FAILED, если это не корневой URL
                log.warn("Страница {} вернула статус {}. Не будет проиндексирована.", url, statusCode);
            }
        } catch (HttpStatusException e) {
            log.error("Ошибка HTTP при обходе страницы {}: Статус {}, Сообщение: {}", url, e.getStatusCode(), e.getMessage());

            if (url.equals(site.getUrl())) { // Если это корневой URL сайта
                site.setStatus(Status.FAILED);
                site.setLastError("Ошибка HTTP при доступе к главной странице: " + e.getStatusCode() + " " + e.getMessage());
                siteRepository.save(site);
            }
        } catch (SSLHandshakeException e) {
            log.error("Ошибка SSL при обходе страницы {}: {}", url, e.getMessage());
            if (url.equals(site.getUrl())) { // Если это корневой URL сайта
                site.setStatus(Status.FAILED);
                site.setLastError("Ошибка SSL-сертификата при доступе к главной странице: " + e.getMessage());
                siteRepository.save(site);
            }
        } catch (SocketTimeoutException e) {
            log.error("Таймаут при обходе страницы {}: {}", url, e.getMessage());
            if (url.equals(site.getUrl())) { // Если это корневой URL сайта
                site.setStatus(Status.FAILED);
                site.setLastError("Таймаут при доступе к главной странице: " + e.getMessage());
                siteRepository.save(site);
            }
        } catch (IOException e) {
            log.error("Ошибка ввода-вывода при обходе страницы {}: {}", url, e.getMessage());
            if (url.equals(site.getUrl())) { // Если это корневой URL сайта
                site.setStatus(Status.FAILED);
                site.setLastError("Ошибка ввода-вывода при доступе к главной странице: " + e.getMessage());
                siteRepository.save(site);
            }
        } catch (Exception e) {
            log.error("Неизвестная ошибка при обходе страницы {}: {}", url, e.getMessage());
            if (url.equals(site.getUrl())) { // Если это корневой URL сайта
                site.setStatus(Status.FAILED);
                site.setLastError("Неизвестная ошибка при доступе к главной странице: " + e.getMessage());
                siteRepository.save(site);
            }
        }
    }
}