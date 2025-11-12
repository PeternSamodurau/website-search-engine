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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Collectors;

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
        String normalizedCurrentUrl = normalizeUrl(url);
        log.debug("Начинаем обход страницы: {}", normalizedCurrentUrl);

        if (!indexingService.isIndexing()) {
            log.debug("Индексация остановлена, прекращаем обход страницы: {}", normalizedCurrentUrl);
            return;
        }
        if (visitedUrls.contains(normalizedCurrentUrl)) {
            log.debug("Страница уже посещена или находится в процессе обхода: {}", normalizedCurrentUrl);
            return;
        }

        visitedUrls.add(normalizedCurrentUrl);

        try {
            Thread.sleep(150); // Задержка для снижения нагрузки на сайт

            Connection.Response response = Jsoup.connect(url) // Используем оригинальный URL для запроса
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .sslSocketFactory(socketFactory()) // Игнорирование SSL ошибок
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();
            String content = response.body();

            // Корректный расчет path относительно нормализованного URL сайта
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
                        log.debug("Обнаружена новая ссылка: {} на странице {}", normalizedAbsUrl, normalizedCurrentUrl);
                        tasks.add(new SiteCrawler(site, absUrl, indexingService, siteRepository, pageRepository, lemmaService, visitedUrls));
                    }
                }
                if (!tasks.isEmpty()) {
                    log.debug("Запускаем {} дочерних задач для страницы: {}", tasks.size(), normalizedCurrentUrl);
                    invokeAll(tasks);
                } else {
                    log.debug("На странице {} не найдено новых ссылок для обхода.", normalizedCurrentUrl);
                }
            } else {
                log.warn("Страница {} вернула код состояния {}. Контент сохранен, но не лемматизирован и не обследован на ссылки.", normalizedCurrentUrl, statusCode);
            }

            updateSiteStatusTime();

        } catch (IOException e) {
            handleError("Ошибка ввода-вывода при обходе страницы " + normalizedCurrentUrl + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleError("Обход страницы " + normalizedCurrentUrl + " прерван.");
        } catch (Exception e) {
            log.error("Неизвестная ошибка при обходе страницы " + normalizedCurrentUrl, e);
            handleError("Неизвестная ошибка: " + e.getMessage());
        }
    }

    private boolean isValidLink(String link) {
        return link.startsWith(normalizedSiteUrl) && // Сравниваем с нормализованным URL сайта
                !link.contains("#") &&
                !visitedUrls.contains(link) && // Проверяем нормализованный URL
                !link.matches(".*\\.(jpg|jpeg|png|gif|bmp|pdf|doc|docx|xls|xlsx|zip|rar|exe|mp3|mp4|avi)$");
    }

    private void updateSiteStatusTime() {
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private void handleError(String errorMessage) {
        log.error(errorMessage);
        site.setLastError(errorMessage);
        site.setStatus(Status.FAILED);
        siteRepository.save(site);
    }

    private String normalizeUrl(String inputUrl) {
        String normalized = inputUrl.toLowerCase();
        // Удаляем "www."
        normalized = normalized.replace("://www.", "://");
        // Удаляем конечный слэш, если это не корень сайта
        if (normalized.endsWith("/") && !normalized.equals(inputUrl.substring(0, inputUrl.indexOf("://") + 3))) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create a SSL socket factory", e);
        }
    }
}