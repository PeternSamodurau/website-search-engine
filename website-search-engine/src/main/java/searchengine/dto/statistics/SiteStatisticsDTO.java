package searchengine.dto.statistics;

import lombok.Data;

@Data
public class SiteStatisticsDTO {
    private String url;
    private String name;
    private String status;
    private long statusTime;
    private String error;
    private int pages;
    private int lemmas;
}
