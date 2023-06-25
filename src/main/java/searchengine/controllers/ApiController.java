package searchengine.controllers;

import org.hibernate.result.Output;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.*;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    public IndexingService indexingService;
    public SitesList sitesList = new SitesList();
    private final StatisticsService statisticsService;
    private volatile boolean isIndexingRunning = false;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        // Получение списка сайтов из конфигурации приложения
        List<searchengine.config.Site> sites1 = new ArrayList<>();
        searchengine.config.Site site1 = new searchengine.config.Site();
        site1.setUrl("http://www.playback.ru/");
        site1.setName("PlayBack");
        sites1.add(site1);
        sitesList.setSites(sites1);

        indexingService = new IndexingService(pageRepository, siteRepository, lemmaRepository, indexRepository, sitesList);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (isIndexingRunning) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Индексация уже запущена"));
        }

        isIndexingRunning = true;
        CompletableFuture.runAsync(() -> {
            //IndexingService indexingService = new IndexingService(pageRepository, siteRepository, lemmaRepository, indexRepository, sitesList);
            indexingService.startIndexing();
            isIndexingRunning = false;
        });

        return ResponseEntity.ok().body(Map.of("result", true));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (!isIndexingRunning) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Индексация не запущена"));
        }
        // Остановка текущей индексации (переиндексации)
        //IndexingService indexingService = new IndexingService(pageRepository, siteRepository, lemmaRepository, indexRepository, sitesList);
        indexingService.stopIndexingProcess();
        isIndexingRunning = false;

        return ResponseEntity.ok().body(Map.of("result", true));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam("url") String url) {
        // Проверка, передан ли сайт
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Неверно указан адрес страницы"));
        }

        // Проверка, находится ли страница в пределах указанных сайтов
        if (!isWebsiteAllowed(url)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"));
        }

        String name = extractWebsiteFromUrl(url);
        Site site = new Site();
        site.setUrl(url);
        site.setName(name);

        List<Site> sites = new ArrayList<>();
        sites.add(site);
        sitesList.setSites(sites);

        // Старт индексации
        startIndexing();

        return ResponseEntity.ok().body(Map.of("result", true));
    }

    @GetMapping("/search")
    private ResponseEntity<Object> search(String query, String siteUrl, int offset, int limit) throws IOException {
        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", false, "error", "Задан пустой поисковый запрос"));
        }
        SearchService searchService = new SearchService(pageRepository, siteRepository, lemmaRepository, indexRepository);
        SearchService.SearchResponse paginatedResponse = searchService.search(query, siteUrl, offset, limit);

        return ResponseEntity.ok().body(Map.of("result", true, "count", paginatedResponse.getCount(), "data", paginatedResponse.getData()));
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

    private boolean isWebsiteAllowed(String website) {
        List<Site> siteList = sitesList.getSites();
        if (siteList == null) return false;
        for (Site site : siteList) {
            if (site.getUrl().equals(website) || website.startsWith(site.getUrl())) return true;
        }
        return false;
    }
}
