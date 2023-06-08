package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.LemmaRepository;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.time.ZoneOffset;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//@Service
//@RequiredArgsConstructor
//public class StatisticsServiceImpl implements StatisticsService {
//
//    private final Random random = new Random();
//    private final SitesList sites;
//    private final SiteRepository siteRepository;
//
//    @Override
//    public StatisticsResponse getStatistics() {
//        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
//        String[] errors = {
//                "Ошибка индексации: главная страница сайта не доступна",
//                "Ошибка индексации: сайт не доступен",
//                ""
//        };
//
//        TotalStatistics total = new TotalStatistics();
//        total.setSites(sites.getSites().size());
//        total.setIndexing(true);
//
//        List<DetailedStatisticsItem> detailed = new ArrayList<>();
//        List<Site> sitesList = sites.getSites();
//        for(int i = 0; i < sitesList.size(); i++) {
//            Site site = sitesList.get(i);
//            searchengine.model.Site site1 = new searchengine.model.Site();
//            site1 = siteRepository.findByName(site.getName());
//            DetailedStatisticsItem item = new DetailedStatisticsItem();
//            item.setName(site.getName());
//            item.setUrl(site.getUrl());
//            int pages = random.nextInt(1_000);
//            int lemmas = pages * random.nextInt(1_000);
//            item.setPages(pages);
//            item.setLemmas(lemmas);
//            item.setStatus(String.valueOf(site1.getStatus()));
//            item.setError(site1.getLastError());
//            item.setStatusTime(site1.getStatusTime().getLong());
//            total.setPages(total.getPages() + pages);
//            total.setLemmas(total.getLemmas() + lemmas);
//            detailed.add(item);
//        }
//
//        StatisticsResponse response = new StatisticsResponse();
//        StatisticsData data = new StatisticsData();
//        data.setTotal(total);
//        data.setDetailed(detailed);
//        response.setStatistics(data);
//        response.setResult(true);
//        return response;
//    }
//}
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = calculateTotalStatistics();
        List<DetailedStatisticsItem> detailed = calculateDetailedStatistics();

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);
        return response;
    }

    private TotalStatistics calculateTotalStatistics() {
        int sitesCount = sites.getSites().size();
        int pagesCount = pageRepository.findAll().size();
        int lemmasCount = lemmaRepository.findAll().size();
        boolean indexing = true;

        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            searchengine.model.Site siteEntity = siteRepository.findByName(site.getName());

            if (siteEntity != null && siteEntity.getStatus() != Status.INDEXING) {
                indexing = false;
            }
        }

        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesCount);
        total.setPages(pagesCount);
        total.setLemmas(lemmasCount);
        total.setIndexing(indexing);
        return total;
    }

    private List<DetailedStatisticsItem> calculateDetailedStatistics() {
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            searchengine.model.Site siteEntity = siteRepository.findByName(site.getName());
            if (siteEntity == null) {
                   continue;
            }
            System.out.println(site.getName());
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(site.getUrl());
            item.setName(site.getName());
            item.setStatus(siteEntity.getStatus().name());
            item.setStatusTime(siteEntity.getStatusTime().toEpochSecond(ZoneOffset.UTC));
            item.setPages(pageRepository.findAllBySiteId(siteEntity.getId()).size());
            item.setLemmas(lemmaRepository.findAllBySite(siteEntity).size());

            if (siteEntity.getStatus() == Status.FAILED) {
                item.setError(siteEntity.getLastError());
            }
            detailed.add(item);
        }
        return detailed;
    }
}
