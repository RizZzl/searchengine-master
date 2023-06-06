package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.IndexRepository;
import searchengine.model.LemmaRepository;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;

    private final StatisticsService statisticsService;
    private boolean isIndexingRunning = false;

    private SitesList sitesList = new SitesList();


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
        IndexingService indexingService = new IndexingService(pageRepository, siteRepository, lemmaRepository, indexRepository);
        indexingService.startIndexing();
        isIndexingRunning = false;

        return ResponseEntity.ok().body(Map.of("result", true));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (!isIndexingRunning) {
            return ResponseEntity.ok()
                    .body(Map.of("result", false, "error", "Индексация не запущена"));
        }
        // Остановка текущей индексации (переиндексации)
        IndexingService indexingService = new IndexingService(pageRepository, siteRepository, lemmaRepository, indexRepository);
        indexingService.stopIndexingProcess();

        return ResponseEntity.ok().body(Map.of("result", true));
    }

    @PostMapping("/api/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam("url") String url) {
        Site site = new Site();
        List<Site> sites = new ArrayList<>();

        // Извлечение имени сайта из URL
        String name = extractWebsiteFromUrl(url);

        site.setUrl(url);
        site.setName(name);
        sites.add(site);
        // Проверка, находится ли страница в пределах указанных сайтов
        if (!isWebsiteAllowed(name)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        }
        sitesList.setSites(sites);

        if (isIndexingRunning) {
            return ResponseEntity.ok().body(Map.of("result", false, "error", "Индексация не запущена"));
        }

        isIndexingRunning = true;
        IndexingService indexingService = new IndexingService(pageRepository, siteRepository, lemmaRepository, indexRepository);
        indexingService.startIndexing();
        isIndexingRunning = false;

        return ResponseEntity.ok().body(Map.of("result", true));
    }

    private String extractWebsiteFromUrl(String url) {
        // Извлечение имени сайта из URL
        String website = null;
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            website = host.startsWith("www.") ? host.substring(4) : host;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return website;
    }

    /////////////////////////////////////////////////
    private boolean isWebsiteAllowed(String website) {
        return true;
    }
    ////////////////////////////////////////////////
}
