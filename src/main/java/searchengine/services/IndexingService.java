package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import searchengine.model.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class IndexingService {
    private List<String> urls;
    private static final String USER_AGENT = "HeliontSearchBot";

    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    public IndexingService(PageRepository pageRepository, SiteRepository siteRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    public void startIndexing() {
        // Получение списка сайтов из конфигурации приложения
        List<String> names = getWebsitesFromConfiguration();
        int count = 0;
        for (String name : names) {
            // Удаление данных по сайту
            siteRepository.deleteByName(name);
            pageRepository.deleteBySite(name);

            String url = urls.get(count);
            count++;
            // Создание новой записи о сайте со статусом INDEXING
            Site site = new Site();
            site.setName(name);
            site.setUrl(url);
            site.setStatus(Status.INDEXING);
            siteRepository.save(site);

            // Запуск индексации сайта в новом потоке
            IndexingTask indexingTask = new IndexingTask(name, site.getId());
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
        List<String> names = new ArrayList<>();
        try {
            Yaml yaml = new Yaml();
            FileInputStream inputStream = new FileInputStream("application.yaml");

            // Чтение файла YAML
            Map<String, Object> data = yaml.load(inputStream);

            // Получение списка сайтов
            List<Map<String, String>> sites = (List<Map<String, String>>) data.get("indexing-settings.sites");

            // Извлечение url и name из каждого сайта
            if (sites != null) { // Проверка, что sites не равно null
                for (Map<String, String> site : sites) {
                    String url = site.get("url");
                    String name = site.get("name");
                    urls.add(url);
                    names.add(name);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return names;
    }

    private void indexPage(String website, String pageUrl) throws IOException, InterruptedException {
        // Получение содержимого страницы и обход ссылок
        Document doc = Jsoup.connect(pageUrl)
                .userAgent(USER_AGENT) // Установка значения User-Agent
                .referrer("http://www.google.com")
                .get();
        String content = doc.html();

        // Сохранение данных страницы в базу данных
        Site site = siteRepository.findByName(website).orElse(null);
        if (site == null) {
            site = new Site();
            site.setName(website);
            site.setUrl(pageUrl);
            site.setStatus(Status.INDEXING);
        }

        Page page = new Page();
        page.setSite(site);
        page.setPath(pageUrl);
        page.setContent(content);
        pageRepository.save(page);

        // Обход ссылок на странице
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");
            indexPage(website, linkUrl);
            // Проверка, был ли уже обработан этот URL
            if (!isPageIndexed(linkUrl)) {
                indexPage(website, linkUrl);
                Thread.sleep(500); // Задержка между запросами к страницам
            }
        }
    }
        private boolean isPageIndexed(String pageUrl) {
            // Проверка, есть ли уже запись о данной странице в базе данных
            return pageRepository.existsByPath(pageUrl);
        }

    private class IndexingTask extends RecursiveAction {
        private final String website;
        private final int siteId;
        private boolean completedSuccessfully;
        private String errorMessage;

        public IndexingTask(String website, int siteId) {
            this.website = website;
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
                indexPage(website, website);

                // Обновление даты и времени статуса сайта
                Site site = siteRepository.findById(siteId).orElse(null).getSite();
                if (site != null) {
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }

                completedSuccessfully = true;
            } catch (Exception e) {
                completedSuccessfully = false;
                errorMessage = e.getMessage();
            }
        }
    }
}
