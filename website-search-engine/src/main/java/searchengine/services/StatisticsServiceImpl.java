package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import searchengine.config.SitesListConfig;
import searchengine.dto.statistics.SiteStatisticsDTO;
import searchengine.dto.statistics.StatisticsDataDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.dto.statistics.TotalStatisticsDTO;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Profile("!init")
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesListConfig sites;
   
    @Override
    public StatisticsResponseDTO getStatistics() {
        TotalStatisticsDTO total = new TotalStatisticsDTO();
        // В будущем: total.setSites(siteRepository.count());
        total.setSites(0);
        total.setIndexing(false); // или реальный статус
        total.setPages(0); // В будущем: pageRepository.count()
        total.setLemmas(0); // В будущем: lemmaRepository.count()

        List<SiteStatisticsDTO> detailed = new ArrayList<>();
        // В будущем здесь будет цикл по сайтам из БД,
        // который будет собирать реальную статистику по каждому

        StatisticsResponseDTO response = new StatisticsResponseDTO();
        StatisticsDataDTO data = new StatisticsDataDTO();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}