package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

@Data
public class StatisticsDataDTO {
    private TotalStatisticsDTO total;
    private List<SiteStatisticsDTO> detailed;
}
