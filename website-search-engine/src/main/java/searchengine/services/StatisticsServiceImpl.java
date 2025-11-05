package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SiteStatisticsDTO;
import searchengine.dto.statistics.StatisticsDataDTO;
import searchengine.dto.statistics.StatisticsResponseDTO;
import searchengine.dto.statistics.TotalStatisticsDTO;
import searchengine.model.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponseDTO getStatistics() {

        TotalStatisticsDTO total = new TotalStatisticsDTO();
        total.setSites((int) siteRepository.count());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(true); // Временная логика, позже заменим на реальный статус

        List<SiteStatisticsDTO> detailed = new ArrayList<>();
        List<Site> sites = siteRepository.findAll();

        for (Site site : sites) {
            SiteStatisticsDTO item = new SiteStatisticsDTO();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            
            // Используем новые методы для точного подсчета
            item.setPages(pageRepository.countPageOnSite(site));
            item.setLemmas(lemmaRepository.countLemmaOnSite(site));
            
            item.setStatus(site.getStatus().toString());
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            
            detailed.add(item);
        }

        StatisticsDataDTO data = new StatisticsDataDTO();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponseDTO response = new StatisticsResponseDTO();
        response.setResult(true);
        response.setStatistics(data);

        return response;
    }
}
