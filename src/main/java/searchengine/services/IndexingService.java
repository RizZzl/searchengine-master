package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.PageDatabase;
import searchengine.model.SiteDatabase;
import searchengine.model.Status;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class IndexingService {
    private final SiteDatabase siteDatabase;
    private final PageDatabase pageDatabase;
    private List<String> urls;

    @Autowired
    public IndexingService(SiteDatabase siteDatabase, PageDatabase pageDatabase) {
        this.siteDatabase = siteDatabase;
        this.pageDatabase = pageDatabase;
    }

    public void startIndexing() {
        // Получение списка сайтов из конфигурации приложения
        List<String> names = getWebsitesFromConfiguration();
        int count = 0; urls.size();
        for (String name : names) {
            // Удаление данных по сайту
            siteDatabase.deleteBySite(name);
            pageDatabase.deleteBySite(name);

            String url = urls.get(count);
            count++;
            // Создание новой записи о сайте со статусом INDEXING
            SiteDatabase site = new SiteDatabase();
            site.setName(name);
            site.setUrl(url);
            site.setStatus(Status.INDEXING);
            siteDatabase.save(site);

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

            siteDatabase.save(site);
        }
    }

    private List<String> getWebsitesFromConfiguration() {
        // Получение списка сайтов из конфигурации приложения
        File file = new File("application.yaml");
        System.out.println(file);
//        urls;
        List<String> names = null;
        return names;
    }

    private void indexPage(String website, String pageUrl) throws IOException {
        // Получение содержимого страницы и обход ссылок
        Document doc = Jsoup.connect(pageUrl).get();
        String content = doc.html();

        // Сохранение данных страницы в базу данных
        SiteDatabase site = new SiteDatabase();
        site.setName(website);

        PageDatabase page = new PageDatabase();
        page.setSite(site);
        page.setPath(pageUrl);
        page.setContent(content);

        // Обход ссылок на странице
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");
            indexPage(website, linkUrl);
        }
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
                SiteDatabase site = (SiteDatabase) siteDatabase;// .findById(siteId).orElse(null);
                if (site != null) {
                    site.setStatusTime(LocalDateTime.now());
                    siteDatabase.save(site);
                }

                completedSuccessfully = true;
            } catch (IOException e) {
                completedSuccessfully = false;
                errorMessage = e.getMessage();
            }
        }
    }
}
