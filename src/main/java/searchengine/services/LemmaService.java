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
    HashMap<String, Integer> lemmaCountMap = new HashMap<>();
    @Autowired
    private final PageRepository pageRepository;

    public LemmaService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public void indexPage(String url) throws IOException {
        Page page = pageRepository.findByPath(url);
        String html = page.getContent();

        // Извлечение текста из HTML-кода
        String text = stripHtmlTags(html);
        countLemmas(text);
    }

    public void countLemmas(String text) throws IOException {
        Pattern pattern = Pattern.compile("\\b[А-Яа-я]+\\b", Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        SortedSet<String> words = new TreeSet<>();
        while (matcher.find()){
            String word = matcher.group().toLowerCase();

            // Проверка, чтобы matcher не был числом
            if (word.matches("-?\\d+(\\.\\d+)?")) {
                continue;
            }
            words.add(word);
        }
        for (String word : words) {
            if (isStopWord(word)) {
                continue;
            }

            for (String lemma : lemmas) {
                lemmaCountMap.put(lemma, lemmaCountMap.getOrDefault(lemma, 0) + 1);
            }
        }
    }

    private boolean isStopWord(String word) throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        List<String> wordBaseForms = luceneMorph.getNormalForms(word);
        if (wordBaseForms.size() < 2){
            lemmas.add(wordBaseForms.get(0));
            return false;
        }
        String secondWord = wordBaseForms.get(1);

        List<String> stopWords = List.of("СОЮЗ", "МЕЖД", "ПРЕДЛ", "ЧАСТ");
        for (String stopWord : stopWords) {
            if (secondWord.endsWith(stopWord)) return true;
        }
        lemmas.add(wordBaseForms.get(0));
        return false;
    }

    public String stripHtmlTags(String html) {
        return Jsoup.parse(html).text();
    }

    public List<String> getLemmas() {
        return lemmas;
    }

    public Map<String, Integer> getLemmaCountMap() {
        return lemmaCountMap;
    }
}
