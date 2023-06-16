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
import java.util.ArrayList;
import java.util.List;

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
        int sitesCount = siteRepository.findAll().size();
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
