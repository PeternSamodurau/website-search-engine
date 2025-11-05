package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);

    @Override
    public boolean startIndexing() {
        if (isIndexing.compareAndSet(false, true)) {
            log.info("Запрос на запуск индексации получен. Индексация запускается.");
            // TODO: Здесь будет логика запуска индексации в отдельном потоке
            return true;
        } else {
            log.warn("Попытка запуска индексации, когда она уже запущена.");
            return false;
        }
    }

    @Override
    public boolean stopIndexing() {
        if (isIndexing.compareAndSet(true, false)) {
            log.info("Запрос на остановку индексации получен. Индексация останавливается.");
            // TODO: Здесь будет логика принудительной остановки индексации
            return true;
        } else {
            log.warn("Попытка остановить индексацию, когда она не была запущена.");
            return false;
        }
    }

    @Override
    public boolean indexPage(String url) {
        log.info("Получен запрос на индексацию отдельной страницы: {}", url);
        // TODO: Здесь будет логика индексации отдельной страницы
        return true; // Пока всегда успешно
    }

    @Override
    public boolean isIndexing() {
        return isIndexing.get();
    }
}
