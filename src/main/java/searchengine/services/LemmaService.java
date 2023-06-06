package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LemmaService {
    List<String> lemmas = new ArrayList<>();

    @Autowired
    private final PageRepository pageRepository;

    public LemmaService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public Map<String, Integer> indexPage(String url) throws IOException {
        Page page = pageRepository.findByPath(url);
        String html = page.getContent();

        // Извлечение текста из HTML-кода
        String text = stripHtmlTags(html);

        return countLemmas(text);
//        for (String lemma : lemmas) {
//            int rank = lemmaCountMap.get(lemma);
//            Lemma lemma1 = new Lemma();
//            lemma1.setLemma(lemma);
//            lemma1.setFrequency();
//            lemma1.setSite(site);
//            lemmaRepository.save(lemma1);
//
//
//            Index index = new Index();
//            index.setPage(page);
//            index.setLemma(lemma1);
//            index.setRank(rank);
//            indexRepository.save(index);
//        }
    }

    public List<String> getLemmas() {
        return lemmas;
    }

    public HashMap<String, Integer> countLemmas(String text) throws IOException {
        HashMap<String, Integer> lemmaCountMap = new HashMap<>();

        Pattern pattern = Pattern.compile("\\w+", Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        SortedSet<String> words = new TreeSet<>();
        while (matcher.find()){
            words.add(matcher.group().toLowerCase());
        }
        for (String word : words) {
            if (isStopWord(word)) {
                continue;
            }

            for (String lemma : lemmas) {
                lemmaCountMap.put(lemma, lemmaCountMap.getOrDefault(lemma, 0) + 1);
            }
        }
        return lemmaCountMap;
    }

    private boolean isStopWord(String word) throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> wordBaseForms = luceneMorph.getNormalForms(word);
        String secondWord = wordBaseForms.get(1);

        List<String> stopWords = List.of("СОЮЗ", "МЕЖД", "ПРЕДЛ", "ЧАСТ");
        for (String stopWord : stopWords) {
            if (secondWord.endsWith(stopWord)) return true;
        }
        lemmas.add(wordBaseForms.get(0));
        return false;
    }

    public String stripHtmlTags(String html) {
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select("*");

        for (Element element : elements) {
            element.unwrap();
        }
        return doc.text();
    }
}
