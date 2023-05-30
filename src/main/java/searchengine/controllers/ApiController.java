package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private PageRepository pageRepository;
    private SiteRepository siteRepository;
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
        if (isIndexingRunning) {
            return ResponseEntity.ok()
                    .body(Map.of("result", false, "error", "Индексация уже запущена"));
        }
        isIndexingRunning = true;
        IndexingService indexingService = new IndexingService(pageRepository, siteRepository);
        indexingService.startIndexing();
        isIndexingRunning = false;
        return ResponseEntity.ok().body(Map.of("result", true));
    }
}
