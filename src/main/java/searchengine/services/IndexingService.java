package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class IndexingService {
    private List<String> urls;
    private static final String USER_AGENT = "HeliontSearchBot";
    private volatile boolean stopIndexingRequested;
    private final SitesList sites = new SitesList();

    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    public IndexingService(PageRepository pageRepository, SiteRepository siteRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    @Transactional
    public void startIndexing() {
        // Получение списка сайтов из конфигурации приложения
        List<searchengine.config.Site> sites1 = new ArrayList<>();
        searchengine.config.Site site1 = new searchengine.config.Site();
        site1.setUrl("http://www.playback.ru/");
        site1.setName("playback");
        sites1.add(site1);
        sites.setSites(sites1);

        List<String> names = getWebsitesFromConfiguration();
        int count = 0;
        for (String name : names) {
            // Удаление данных по сайту
            Site siteDel = siteRepository.findByName(name);
            if (siteDel != null) {
                pageRepository.deleteBySiteId(siteDel.getId());
                siteRepository.deleteByName(siteDel.getName());
            }

            String url = urls.get(count);
            count++;
            // Создание новой записи о сайте со статусом INDEXING
            Site site = new Site();
            site.setName(name);
            site.setUrl(url);
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXING);
            siteRepository.save(site);

            // Запуск индексации сайта в новом потоке
            IndexingTask indexingTask = new IndexingTask(name, url, site.getId());
            ForkJoinPool.commonPool().invoke(indexingTask);

            // Обновление статуса сайта на INDEXED или FAILED по завершении индексации
            if (indexingTask.isCompletedSuccessfully()) {
                site.setStatus(Status.INDEXED);
            } else {
                site.setStatus(Status.FAILED);
                site.setLastError(indexingTask.getErrorMessage());
            }
            siteRepository.save(site);
        }
    }

    private List<String> getWebsitesFromConfiguration() {
        // Получение списка сайтов из конфигурации приложения
        List<searchengine.config.Site> sitesList = sites.getSites();
        List<String> names = new ArrayList<>();
        urls = new ArrayList<>();
        if (sitesList!=null) {
            for (searchengine.config.Site site : sitesList) {
                urls.add(site.getUrl());
                names.add(site.getName());
            }
        }
        return names;
    }

    private void indexPage(String website, String pageUrl) throws IOException, InterruptedException {
        // Получение содержимого страницы и обход ссылок
        Document doc = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT)
                .referrer("http://www.google.com")
                .get();
        String content = doc.html();

        // Сохранение данных страницы в базу данных
        Site site = siteRepository.findByName(website);
        if (site == null) {
            site = new Site();
            site.setName(website);
            site.setUrl(pageUrl);
            site.setStatus(Status.INDEXING);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }

        Page page = new Page();
        page.setSite(site);
        if (!pageUrl.equals(site.getUrl())){
            page.setPath(pageUrl.replace(site.getUrl(), ""));
        }
        page.setCode(getResponseCode(pageUrl));
        page.setContent(content);
        pageRepository.save(page);

        // Обход ссылок на странице
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");
            // Проверка, был ли уже обработан этот URL
            System.out.println(linkUrl);
            System.out.println(!isPageIndexed(linkUrl));
            if (!isPageIndexed(linkUrl) && linkUrl.startsWith(site.getUrl())) {
                indexPage(website, linkUrl);
                Thread.sleep(500); // Задержка между запросами к страницам
            }
        }
    }

    private boolean isPageIndexed(String pageUrl) {
        // Проверка, есть ли уже запись о данной странице в базе данных
        return pageRepository.existsByPath(pageUrl);
    }

    private int getResponseCode(String pageUrl) throws IOException {
        // Получение кода ответа страницы
        Connection.Response response = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT)
                .referrer("http://www.google.com")
                .execute();
        return response.statusCode();
    }

    public void stopIndexingProcess() {
        stopIndexingRequested = true;
    }

    private class IndexingTask extends RecursiveAction {
        private final String name;
        private final String url;
        private final int siteId;
        private boolean completedSuccessfully;
        private String errorMessage;

        public IndexingTask(String name, String url, int siteId) {
            this.name = name;
            this.url = url;
            this.siteId = siteId;
        }

        public boolean isCompletedSuccessfully() {
            return completedSuccessfully;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        protected void compute() {
            try {
                // Обход главной страницы и индексация
                indexPage(name, url);

                // Обновление даты и времени статуса сайта
                Site site = siteRepository.findById(siteId).orElse(null).getSite();
                if (site != null) {
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }

                completedSuccessfully = true;
            } catch (Exception e) {
                System.out.println("error");
                completedSuccessfully = false;
                errorMessage = e.getMessage();
            } finally {
                if (stopIndexingRequested) {
                    // Если была запрошена остановка индексации, записываем состояние FAILED и текст ошибки
                    Site site = siteRepository.findById(siteId).orElse(null).getSite();
                    if (site != null) {
                        site.setStatus(Status.FAILED);
                        site.setLastError("Индексация остановлена пользователем");
                        siteRepository.save(site);
                    }
                }
            }
        }
    }
}
