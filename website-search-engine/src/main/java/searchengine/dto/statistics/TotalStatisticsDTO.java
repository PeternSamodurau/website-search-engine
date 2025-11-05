package searchengine.dto.statistics;

import lombok.Data;

@Data
public class TotalStatisticsDTO {
    private int sites;
    private int pages;
    private int lemmas;
    private boolean indexing;
}
