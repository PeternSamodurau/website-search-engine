package searchengine.init;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.SiteStatisticsDTO;
import searchengine.dto.statistics.StatisticsDataDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.dto.statistics.TotalStatisticsDTO;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Profile("init")
@Slf4j
public class StatisticsServiceInitImpl implements StatisticsService {

    private final SitesListConfig sites;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    // Метод для инициализации сайтов при старте приложения в init профиле
    @PostConstruct
    @Transactional
    public void initializeSites() {
        log.info("Инициализация/обновление сайтов для 'init' профиля...");

        // Удаляем сайты из БД, которых нет в конфигурации
        List<Site> sitesInDb = siteRepository.findAll();
        for (Site siteInDb : sitesInDb) {
            boolean foundInConfig = false;
            for (SiteConfig siteConfig : sites.getSites()) {
                if (siteInDb.getUrl().equals(siteConfig.getUrl())) {
                    foundInConfig = true;
                    break;
                }
            }
            if (!foundInConfig) {
                log.info("Удаление сайта из БД, отсутствующего в конфигурации: {}", siteInDb.getName());
                lemmaRepository.deleteAllBySite(siteInDb);
                pageRepository.deleteAllBySite(siteInDb);
                siteRepository.delete(siteInDb);
            }
        }


        for (SiteConfig siteConfig : sites.getSites()) {
            Optional<Site> existingSiteOptional = siteRepository.findByUrl(siteConfig.getUrl());
            Site site;

            if (existingSiteOptional.isPresent()) {
                site = existingSiteOptional.get();
                log.info("Обновление существующего сайта: {}", siteConfig.getName());
            } else {
                site = new Site();
                log.info("Создание нового сайта: {}", siteConfig.getName());
            }

            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl());
            site.setStatusTime(LocalDateTime.now());

            if (isSiteAvailable(siteConfig.getUrl())) {
                site.setStatus(Status.INDEXING); // Доступен, готов к индексации
                site.setLastError(null);
                log.info("Сайт '{}' доступен. Статус: INDEXING", siteConfig.getName());
            } else {
                site.setStatus(Status.FAILED); // Недоступен
                site.setLastError("Сайт недоступен или произошла ошибка при проверке.");
                log.warn("Сайт '{}' недоступен. Статус: FAILED", siteConfig.getName());
            }
            siteRepository.save(site);
        }
        log.info("Инициализация/обновление сайтов завершено.");
    }

    @Override
    public StatisticsResponseDTO getStatistics() {
        log.info("Запрос на получение статистики для 'init' профиля");

        TotalStatisticsDTO total = new TotalStatisticsDTO();
        total.setSites((int) siteRepository.count());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(indexingService.isIndexing());

        List<SiteStatisticsDTO> detailed = new ArrayList<>();
        List<Site> siteModels = siteRepository.findAll(); // Получаем реальные сайты из базы

        for (Site siteModel : siteModels) {
            SiteStatisticsDTO item = new SiteStatisticsDTO();
            item.setName(siteModel.getName());
            item.setUrl(siteModel.getUrl());
            item.setPages(pageRepository.countBySiteId(siteModel.getId()));
            
            // ИСПРАВЛЕНО: Явное преобразование типа
            item.setLemmas(lemmaRepository.countBySite(siteModel));
            
            item.setStatus(siteModel.getStatus().toString());
            item.setError(siteModel.getLastError() == null ? "Ошибок нет" : siteModel.getLastError());

            item.setStatusTime(siteModel.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            detailed.add(item);
        }

        StatisticsResponseDTO response = new StatisticsResponseDTO();
        StatisticsDataDTO data = new StatisticsDataDTO();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);

        log.info("Статистика для 'init' профиля успешно собрана. Всего сайтов: {}, страниц: {}, лемм: {}",
                total.getSites(), total.getPages(), total.getLemmas());

        return response;
    }

    // Метод для проверки доступности сайта
    private boolean isSiteAvailable(String siteUrl) {
        try {
            URL url = new URL(siteUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // Используем HEAD запрос для проверки доступности
            connection.setConnectTimeout(5000); // Таймаут на подключение 5 секунд
            connection.setReadTimeout(5000);    // Таймаут на чтение 5 секунд
            int responseCode = connection.getResponseCode();
            // Считаем сайт доступным, если код ответа 2xx или 3xx
            return (responseCode >= 200 && responseCode < 400);
        } catch (IOException e) {
            log.error("Ошибка при проверке доступности сайта {}: {}", siteUrl, e.getMessage());
            return false;
        }
    }
}