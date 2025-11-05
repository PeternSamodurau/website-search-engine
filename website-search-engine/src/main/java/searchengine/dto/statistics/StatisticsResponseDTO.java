package searchengine.dto.statistics;

import lombok.Data;

@Data
public class StatisticsResponseDTO {
    private boolean result;
    private StatisticsDataDTO statistics;
}
