package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.services.StatisticsService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final StatisticsResponse statisticsResponse;

    public ApiController(StatisticsService statisticsService, StatisticsResponse statisticsResponse) {
        this.statisticsService = statisticsService;
        this.statisticsResponse = statisticsResponse;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public void startIndexing() {
        StatisticsResponse statisticsResponse = statisticsService.getStatistics();
        StatisticsData statisticsData = statisticsResponse.getStatistics();
        TotalStatistics totalStatistics = statisticsData.getTotal();
        boolean indexing = totalStatistics.getIndexing();
        if (indexing) {
            statisticsResponse.setResult(false);
            StatisticsData statisticsData1 = statisticsResponse.getStatistics();
            List<DetailedStatisticsItem> list = statisticsData1.getDetailed();
            DetailedStatisticsItem detailedStatisticsItem = list.get(1);
            detailedStatisticsItem.setError("Индексация уже запущена");
        }
        statisticsResponse.setResult(true);

    }
}
