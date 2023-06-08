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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class IndexingService {
    private List<String> urls;
    private static final String USER_AGENT = "HeliontSearchBot";
    private volatile boolean stopIndexingRequested;
    private final SitesList sites = new SitesList();
    // private LemmaService lemmaService;
    private Map<String, Integer> lemmaCountMap = new HashMap<>();

    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;

    @Autowired
    public IndexingService(PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Transactional
    public void startIndexing() {
        // Получение списка сайтов из конфигурации приложения
        List<searchengine.config.Site> sites1 = new ArrayList<>();
        searchengine.config.Site site1 = new searchengine.config.Site();
        site1.setUrl("http://www.playback.ru/");
        site1.setName("PlayBack");
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
        if (isPageIndexed(pageUrl)) {
            // Если страница уже проиндексирована, удаление информации о ней
            deleteIndexedPage(pageUrl);
        }

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
        } else {
            page.setPath(pageUrl);
        }
        page.setCode(getResponseCode(pageUrl));
        page.setContent(content);
        pageRepository.save(page);

        LemmaService lemmaService = new LemmaService(pageRepository);
        lemmaCountMap = lemmaService.getLemmaCountMap();

        lemmaService.indexPage(pageUrl);

        updateLemmaAndIndex(lemmaService.getLemmas(), page);

        // Обход ссылок на странице
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");
            // Проверка, был ли уже обработан этот URL
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

    private void updateLemmaAndIndex(List<String> lemmas, Page page) {
        for (String lemma : lemmas) {
            // Поиск леммы в базе данных
            Site site = siteRepository.findById(page.getSite());
            Lemma existingLemma = lemmaRepository.findByLemma(lemma);
            if (existingLemma == null) {
                // Леммы нет в базе данных, добавляем новую запись
                Lemma newLemma = new Lemma();
                newLemma.setSite(site);
                newLemma.setLemma(lemma);
                newLemma.setFrequency(1);
                lemmaRepository.save(newLemma);
                System.out.println("New Lemma add"); ///////////

                // Создание записи в индексе
                Index newIndex = new Index();
                newIndex.setLemma(newLemma);
                newIndex.setPage(page);
                newIndex.setRank(lemmaCountMap.get(existingLemma));
                indexRepository.save(newIndex);
                System.out.println("New Index add"); //////////////
            } else {
                // Лемма уже существует в базе данных, увеличиваем ее frequency
                existingLemma.setFrequency(existingLemma.getFrequency() + 1);
                lemmaRepository.save(existingLemma);

                // Проверка наличия записи в индексе для данной леммы и страницы
                Index existingIndex = indexRepository.findByLemmaAndPage(existingLemma, page);
                if (existingIndex == null) {
                    // Записи в индексе нет, создаем новую запись
                    Index newIndex = new Index();
                    newIndex.setLemma(existingLemma);
                    newIndex.setPage(page);
                    newIndex.setRank(lemmaCountMap.get(existingLemma));
                    indexRepository.save(newIndex);
                    System.out.println("Add index"); /////////////
                } else {
                    // Запись в индексе уже существует, увеличиваем ее ранг
                    existingIndex.setRank(existingIndex.getRank() + lemmaCountMap.get(existingLemma));
                    indexRepository.save(existingIndex);
                    System.out.println("Index add"); /////////////
                }
            }
        }
    }

    private void deleteIndexedPage(String pageUrl) {
        // Удаление информации о странице из таблиц page, lemma и index
        Page page = pageRepository.findByPath(pageUrl);
        if (page != null) {
            Site site = siteRepository.findById(page.getSite());
            // Удаление связей в таблице index
            indexRepository.deleteByPage(page);

            // Удаление связей в таблице lemma
            List<Lemma> lemmas = lemmaRepository.findAllBySite(site);
            for (Lemma lemma : lemmas) {
                lemmaRepository.delete(lemma);
            }

            // Удаление записи о странице из таблицы page
            pageRepository.delete(page);
        }
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
                System.out.println("error: " + e); /////
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
