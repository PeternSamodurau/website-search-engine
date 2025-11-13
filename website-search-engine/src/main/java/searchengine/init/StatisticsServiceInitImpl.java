package searchengine.init;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.CrawlerConfig;
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
    private final CrawlerConfig crawlerConfig;

    @PostConstruct
    @Transactional
    public void initializeSites() {
        log.info("Инициализация/обновление сайтов для 'init' профиля...");

        List<Site> sitesInDb = siteRepository.findAll();
        for (Site siteInDb : sitesInDb) {
            boolean foundInConfig = sites.getSites().stream()
                    .anyMatch(sc -> sc.getUrl().equals(siteInDb.getUrl()));
            if (!foundInConfig) {
                log.info("Удаление сайта из БД, отсутствующего в конфигурации: {}", siteInDb.getName());
                lemmaRepository.deleteAllBySite(siteInDb);
                pageRepository.deleteAllBySite(siteInDb);
                siteRepository.delete(siteInDb);
            }
        }

        for (SiteConfig siteConfig : sites.getSites()) {
            Site site = siteRepository.findByUrl(siteConfig.getUrl()).orElseGet(() -> {
                log.info("Создание нового сайта: {}", siteConfig.getName());
                Site newSite = new Site();
                newSite.setName(siteConfig.getName());
                newSite.setUrl(siteConfig.getUrl());
                return newSite;
            });

            site.setStatusTime(LocalDateTime.now());

            if (isSiteAvailable(siteConfig.getUrl())) {
                site.setStatus(Status.INDEXING);
                site.setLastError(null);
                log.info("Сайт '{}' доступен. Статус: INDEXING", siteConfig.getName());
            } else {
                site.setStatus(Status.FAILED);
                site.setLastError("Сайт недоступен или произошла ошибка при проверке.");
                // Этот лог теперь будет менее важным, так как детальная информация будет в isSiteAvailable
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
        List<Site> siteModels = siteRepository.findAll();

        for (Site siteModel : siteModels) {
            SiteStatisticsDTO item = new SiteStatisticsDTO();
            item.setName(siteModel.getName());
            item.setUrl(siteModel.getUrl());
            item.setPages(pageRepository.countBySiteId(siteModel.getId()));
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

    // ИЗМЕНЕНО: Добавлено детальное логирование для отладки
    private boolean isSiteAvailable(String siteUrl) {
        try {
            log.info("Проверка доступности сайта: {}", siteUrl);
            Connection.Response response = Jsoup.connect(siteUrl)
                    .userAgent(crawlerConfig.getUserAgent())
                    .referrer(crawlerConfig.getReferrer())
                    .ignoreHttpErrors(true)
                    .timeout(10000)
                    .execute();

            int responseCode = response.statusCode();
            log.info("Сайт {} вернул код ответа: {}", siteUrl, responseCode);

            boolean isAvailable = (responseCode >= 200 && responseCode < 400);
            if (!isAvailable) {
                String body = response.body();
                log.warn("Сайт {} недоступен. Код: {}. Тело ответа (первые 500 символов): {}",
                        siteUrl, responseCode, body.substring(0, Math.min(body.length(), 500)));
            }
            return isAvailable;
        } catch (IOException e) {
            log.error("Ошибка IO при проверке доступности сайта {}: {}", siteUrl, e.getMessage());
            return false;
        }
    }
}