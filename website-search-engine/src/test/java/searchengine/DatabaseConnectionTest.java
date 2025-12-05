package searchengine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// 1. Запускаем тест Spring Boot
@SpringBootTest(
    // 2. Явно указываем, что нужно запускать НАШЕ тестовое приложение, а не основное
    classes = WebsiteSearchEngineTestApplication.class
)
// 3. Импортируем конфигурацию с контейнером (с новым, правильным именем)
@Import(TestcontainersConfiguration.class)
class DatabaseConnectionTest {

    @Test
    void contextLoads() {
        // Этот тест проверит, что:
        // 1. TestApplication запустилось.
        // 2. Контейнер PostgreSQL запустился.
        // 3. Spring Boot автоматически подключился к этому контейнеру.
        // Если тест "зеленый", значит, все работает.
    }
}
