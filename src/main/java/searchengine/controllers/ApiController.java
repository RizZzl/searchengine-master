package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.services.StatisticsService;

import java.io.Serializable;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private boolean isIndexingRunning;


    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (isIndexingRunning()) {
            return ResponseEntity.ok()
                    .body(Map.of("result", false, "error", "Индексация уже запущена"));
        }
        isIndexingRunning = true;

        return ResponseEntity.ok()
                .body(Map.of("result", true));
    }

    public boolean isIndexingRunning() {
        StatisticsResponse statisticsResponse = statisticsService.getStatistics();
        StatisticsData statisticsData = statisticsResponse.getStatistics();
        TotalStatistics totalStatistics = statisticsData.getTotal();
        isIndexingRunning = totalStatistics.getIndexing();
        return isIndexingRunning;
    }
}
