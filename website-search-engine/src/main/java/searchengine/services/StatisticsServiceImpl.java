package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.SiteStatisticsDTO;
import searchengine.dto.statistics.StatisticsDataDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.dto.statistics.TotalStatisticsDTO;
import searchengine.model.Site;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesListConfig sites;
    private final IndexingService indexingService;

    @Override
    public StatisticsResponseDTO getStatistics() {
        log.info("Запрос на получение статистики");

        TotalStatisticsDTO total = new TotalStatisticsDTO();
        long totalSitesCount = siteRepository.count();
        long totalPagesCount = pageRepository.count(); // Получаем общее количество страниц
        long totalLemmasCount = lemmaRepository.count(); // Получаем общее количество лемм

        log.info("DEBUG: siteRepository.count() = {}", totalSitesCount);
        log.info("DEBUG: pageRepository.count() = {}", totalPagesCount);
        log.info("DEBUG: lemmaRepository.count() = {}", totalLemmasCount);

        total.setSites((int) totalSitesCount);
        total.setPages((int) totalPagesCount);
        total.setLemmas((int) totalLemmasCount);
        total.setIndexing(indexingService.isIndexing());

        List<SiteStatisticsDTO> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();

        for (SiteConfig siteConfig : sitesList) {
            log.info("Обработка сайта из конфигурации: {}", siteConfig.getName());

            Optional<Site> siteModelOpt = siteRepository.findByUrl(siteConfig.getUrl());

            if (siteModelOpt.isEmpty()) {
                log.warn("Сайт '{}' с URL '{}' не найден в базе данных. Пропускаем.", siteConfig.getName(), siteConfig.getUrl());
                continue;
            }
            Site siteModel = siteModelOpt.get();

            long sitePagesCount = pageRepository.countBySiteId(siteModel.getId()); // Получаем страницы для конкретного сайта
            long siteLemmasCount = lemmaRepository.countBySite(siteModel); // Получаем леммы для конкретного сайта

            log.info("DEBUG: Для сайта '{}' (ID: {}): страниц = {}, лемм = {}",
                    siteModel.getName(), siteModel.getId(), sitePagesCount, siteLemmasCount);

            SiteStatisticsDTO item = new SiteStatisticsDTO();
            item.setName(siteModel.getName());
            item.setUrl(siteModel.getUrl());
            item.setPages((int) sitePagesCount);
            item.setLemmas((int) siteLemmasCount);
            item.setStatus(siteModel.getStatus().toString());
            item.setError(siteModel.getLastError() == null ? "Ошибок нет" : siteModel.getLastError());
            item.setStatusTime(siteModel.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            detailed.add(item);
        }

        StatisticsDataDTO data = new StatisticsDataDTO();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponseDTO response = new StatisticsResponseDTO();
        response.setStatistics(data);
        response.setResult(true);

        log.info("Статистика успешно собрана. Всего сайтов: {}, страниц: {}, лемм: {}",
                total.getSites(), total.getPages(), total.getLemmas());

        return response;
    }
}