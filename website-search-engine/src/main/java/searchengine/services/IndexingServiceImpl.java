package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig; // Может быть удален, если SitesListConfig больше не используется напрямую
import searchengine.config.SitesListConfig;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaService lemmaService;
    private final SitesListConfig sites; // Оставлен, так как используется для инициализации в init профиле

    private final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;

    @Override
    public boolean startIndexing() {
        if (isIndexing.compareAndSet(false, true)) {
            log.info("Запуск процесса индексации");

            new Thread(() -> {
                try {
                    // Получаем сайты для индексации из базы данных, которые имеют статус INDEXING
                    List<Site> sitesToProcess = siteRepository.findAllByStatus(Status.INDEXING);

                    if (sitesToProcess.isEmpty()) {
                        log.warn("Нет сайтов со статусом INDEXING для обработки. Процесс индексации завершен.");
                        isIndexing.set(false); // Сбрасываем флаг, если нет сайтов для индексации
                        return;
                    }

                    forkJoinPool = new ForkJoinPool();
                    Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

                    for (Site site : sitesToProcess) { // Итерируем по объектам Site из базы данных
                        // Проверка на остановку индексации перед обработкой каждого сайта
                        if (!isIndexing.get()) {
                            log.info("Индексация остановлена пользователем. Пропускаем оставшиеся сайты.");
                            break; // Выходим из цикла, если индексация остановлена
                        }

                        // Обновляем статус сайта и очищаем предыдущие данные
                        updateSiteForIndexing(site); // Устанавливаем статус INDEXING и время
                        clearSiteData(site);         // Очищаем страницы и леммы

                        log.info("Запуск индексации для сайта: {}", site.getName());

                        // Передаем существующий объект Site в SiteCrawler
                        SiteCrawler task = new SiteCrawler(site, site.getUrl(), this, siteRepository, pageRepository, lemmaService, visitedUrls);
                        forkJoinPool.invoke(task);

                        // После завершения обхода, обновляем статус сайта
                        if (isIndexing.get()) { // Только если индексация не была остановлена
                            site.setStatus(Status.INDEXED);
                            site.setLastError(null);
                        } else {
                            // Если индексация была остановлена, метод stopIndexing() установит статус FAILED
                        }
                        site.setStatusTime(LocalDateTime.now());
                        siteRepository.save(site);

                        log.info("Индексация сайта '{}' завершена.", site.getName());
                    }
                } catch (Exception e) {
                    log.error("Критическая ошибка во время процесса индексации", e);
                } finally {
                    if (forkJoinPool != null) {
                        forkJoinPool.shutdown();
                    }
                    isIndexing.set(false);
                    log.info("Процесс индексации ВСЕХ сайтов завершен.");
                }
            }, "Indexing-Manager-Thread").start();

            return true;
        } else {
            log.warn("Попытка запуска индексации, когда она уже запущена");
            return false;
        }
    }

    // Вспомогательный метод для обновления статуса сайта перед индексацией
    @Transactional
    private void updateSiteForIndexing(Site site) {
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(null); // Очищаем предыдущие ошибки
        siteRepository.save(site);
    }

    // Вспомогательный метод для очистки данных существующего сайта
    @Transactional
    private void clearSiteData(Site site) {
        log.info("Очистка старых данных для сайта: {}", site.getName());
        lemmaRepository.deleteAllBySite(site);
        pageRepository.deleteAllBySite(site);
    }

    // Методы deleteSiteData(SiteConfig siteConfig) и createSite(SiteConfig siteConfig) удалены,
    // так как их логика интегрирована или заменена в новом потоке индексации.

    @Override
    public boolean stopIndexing() {
        if (!isIndexing.get()) {
            log.warn("Попытка остановить индексацию, когда она не запущена");
            return false;
        }

        log.info("Остановка процесса индексации");
        if (forkJoinPool != null && !forkJoinPool.isShutdown()) {
            forkJoinPool.shutdownNow();
        }
        isIndexing.set(false);

        // Обновляем только те сайты, которые были в процессе индексации
        siteRepository.findAllByStatus(Status.INDEXING).forEach(site -> {
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            siteRepository.save(site);
        });
        return true;
    }

    @Override
    public boolean isIndexing() {
        return isIndexing.get();
    }

    @Override
    public boolean indexPage(String url) {
        log.warn("Метод indexPage в данный момент не поддерживается. Используйте полную индексацию.");
        return false;
    }
}