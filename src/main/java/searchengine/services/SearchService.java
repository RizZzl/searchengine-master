package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SearchService {
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;

    public SearchService(PageRepository pageRepository, SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public List<SearchService.SearchResult> search(String query, String siteUrl, int offset, int limit) throws IOException {
        if (siteUrl.equals("All sites")) {
            List<Site> siteList = siteRepository.findAll();
            for (Site site : siteList) {
                search(query, site.getUrl(), offset, limit);
            }
        }

        searchengine.model.Site site = siteRepository.findByUrl(siteUrl);

        List<String> lemmas = getLemmas(query);

        List<String> pages = new ArrayList<>();
        for (String lemma : lemmas) {
            List<String> matchedPages = new ArrayList<>();
            if (site != null) {
                matchedPages = searchPagesByLemma(lemma, site.getUrl());
            }
            if (pages.isEmpty()) {
                pages.addAll(matchedPages);
            } else {
                pages.retainAll(matchedPages);
            }
        }

        List<SearchService.SearchResult> searchResults = calculateRelevanceAndSortPages(pages, lemmas);

        int startIndex = Math.min(offset, searchResults.size());
        int endIndex = Math.min(offset + limit, searchResults.size());
        List<SearchService.SearchResult> paginatedResults = searchResults.subList(startIndex, endIndex);

        return paginatedResults;
    }

    public List<String> getLemmas(String query) throws IOException {
        Pattern pattern = Pattern.compile("\\b[А-Яа-я]+\\b", Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        SortedSet<String> words = new TreeSet<>();
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            words.add(word);
        }

        List<String> listLemma = new ArrayList<>();
        for (String word : words) {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            List<String> wordBaseForms = luceneMorph.getNormalForms(word);
            String secondWord;
            if (wordBaseForms.size() < 2) {
                secondWord = wordBaseForms.get(0);
            } else {
                secondWord = wordBaseForms.get(1);
            }

            List<String> stopWords = List.of("СОЮЗ", "МЕЖД", "ПРЕДЛ", "ЧАСТ");
            boolean isStopWord = false;
            for (String stopWord : stopWords) {
                if (secondWord.endsWith(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }

            if (!isStopWord) {
                listLemma.add(wordBaseForms.get(0));
            }
        }

        return listLemma;
    }

    public List<SearchResult> calculateRelevanceAndSortPages(List<String> pages, List<String> lemmas) {
        List<SearchService.SearchResult> searchResults = new ArrayList<>();

        for (String pageUrl : pages) {
            Page page = pageRepository.findByPath(pageUrl);
            float totalRank = 0.0f;

            for (String lemma : lemmas) {
                Lemma lemmaObj = lemmaRepository.findByLemmaAndSiteId(lemma, page.getSite().getId());
                Index index = indexRepository.findByPageAndLemma(page, lemmaObj);
                if (index != null) {
                    totalRank += index.getRank();
                }
            }

            float maxRank = getMaxRank(pages, lemmas);
            float relevance = totalRank / maxRank;

            SearchService.SearchResult result = new SearchService.SearchResult();
            result.setUri(page.getPath());
            result.setTitle(Jsoup.parse(page.getContent()).title());
            result.setSnippet(getSnippetFromPage(Jsoup.parse(page.getContent()).text(), lemmas));
            result.setRelevance(relevance);

            searchResults.add(result);
        }

        searchResults.sort(Comparator.comparing(SearchService.SearchResult::getRelevance).reversed());

        return searchResults;
    }

    public List<String> searchPagesByLemma(String lemma, String siteUrl) {
        List<String> matchedPages = new ArrayList<>();

        searchengine.model.Site site = siteRepository.findByUrl(siteUrl);
        List<Page> pageList = pageRepository.findAllBySiteId(site);
        Lemma lemma1 = lemmaRepository.findByLemma(lemma);

        for (Page page : pageList) {
            Index index = indexRepository.findByPageAndLemma(page, lemma1);
            matchedPages.add(index.getPage().getPath());
        }

        return matchedPages;
    }

    public String getSnippetFromPage(String content, List<String> lemmas) {
        String snippet = "";
        String[] lines = content.split("\\r?\\n");

        int snippetLineCount = 3; // Количество строк в отрывке
        int snippetMaxLength = snippetLineCount * 100; // Максимальная длина отрывка

        StringBuilder snippetBuilder = new StringBuilder();
        for (String line : lines) {
            if (snippetBuilder.length() >= snippetMaxLength) {
                break;
            }

            if (lineContainsLemma(line, lemmas)) {
                line = highlightLemmaMatches(line, lemmas);
                snippetBuilder.append(line).append("\n");
            }
        }

        snippet = snippetBuilder.toString();
        return snippet;
    }

    public boolean lineContainsLemma(String line, List<String> lemmas) {
        for (String lemma : lemmas) {
            if (line.toLowerCase().contains(lemma.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String highlightLemmaMatches(String line, List<String> lemmas) {
        for (String lemma : lemmas) {
            String highlightedLemma = "<b>" + lemma + "</b>";
            line = line.replaceAll("(?i)\\b" + lemma + "\\b", highlightedLemma);
        }
        return line;
    }

    public float getMaxRank(List<String> pages, List<String> lemmas) {
        float maxRank = 0.0f;

        for (String pageUrl : pages) {
            Page page = pageRepository.findByPath(pageUrl);
            float totalRank = 0.0f;

            for (String lemma : lemmas) {
                Lemma lemmaObj = lemmaRepository.findByLemmaAndSiteId(lemma, page.getSite().getId());
                Index index = indexRepository.findByPageAndLemma(page, lemmaObj);
                if (index != null) {
                    totalRank += index.getRank();
                }
            }

            if (totalRank > maxRank) {
                maxRank = totalRank;
            }
        }
        return maxRank;
    }

    @Getter
    @Setter
    public class SearchResult {
        private String uri;
        private String title;
        private String snippet;
        private float relevance;
    }
}

