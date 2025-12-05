package searchengine;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = SitesListConfig.class)
// ИЗМЕНЕНИЕ:
// Мы больше не зависим от внешнего application.properties.
// Мы явно указываем нужные нам свойства ПРЯМО ЗДЕСЬ.
// Это гарантирует, что тест всегда получит то, что ему нужно.
@TestPropertySource(properties = {
        "indexing-settings.sites[0].url=http://quotes.toscrape.com",
        "indexing-settings.sites[0].name=Quotes To Scrape"
})
@Slf4j
class SiteConnectionTest {

    @Autowired
    private SitesListConfig sitesConfig;

    @Test
    @DisplayName("Проверка успешного подключения к тестовому сайту")
    void connectionToTestSiteShouldBeSuccessful() throws IOException {
        log.info("Тест запущен. Попытка получить конфигурацию сайтов...");

        assertNotNull(sitesConfig.getSites(), "Конфигурация сайтов не должна быть null");
        log.info("Загружено {} сайтов в конфигурации.", sitesConfig.getSites().size());

        SiteConfig testSite = sitesConfig.getSites().get(0);
        String testSiteUrl = testSite.getUrl();

        log.info("Попытка подключения к URL: '{}'", testSiteUrl);

        Connection.Response response = Jsoup.connect(testSiteUrl)
                .userAgent("TestSearchBot/1.0")
                .execute();

        assertEquals(200, response.statusCode());

        log.info("Подключение к '{}' успешно. Статус: {}", testSiteUrl, response.statusCode());
    }
}