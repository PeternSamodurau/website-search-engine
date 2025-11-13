package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
@Profile("!init")
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesListConfig sites;
    private final IndexingService indexingService; // <-- ДОБАВЛЕНА ЗАВИСИМОСТЬ

    @Override
    public StatisticsResponseDTO getStatistics() {
        log.info("Запрос на получение статистики");

        TotalStatisticsDTO total = new TotalStatisticsDTO();
        total.setSites((int) siteRepository.count());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(indexingService.isIndexing()); // <-- ИСПРАВЛЕНО: Используется реальный статус

        List<SiteStatisticsDTO> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSites();

        for (SiteConfig siteConfig : sitesList) {
            log.info("Обработка сайта из конфигурации: {}", siteConfig.getName());

            Optional<Site> siteModelOpt = siteRepository.findAll().stream()
                    .filter(s -> s.getName().equals(siteConfig.getName()))
                    .findFirst();

            if (siteModelOpt.isEmpty()) {
                log.warn("Сайт '{}' не найден в базе данных. Пропускаем.", siteConfig.getName());
                continue;
            }
            Site siteModel = siteModelOpt.get();

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