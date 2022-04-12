package main.search;

import main.lemmatizer.Lemmatizer;
import main.model.Lemma;
import main.model.Page;
import main.model.Site;
import main.repositories.IndexRepository;
import main.repositories.LemmaRepository;
import main.repositories.PageRepository;
import main.repositories.SiteRepository;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Search {

    private static final float MAX_FREQUENCY_IN_PERCENTS = 95; //max 100%

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    public List<SearchResult> searching(String searchQueryText, Site site) {

        List<Page> pages;

        if(site == null) {
            pages = pageRepository.findAll();
        } else {
            pages = pageRepository.findPageBySite(site);
        }

        List<Lemma> searchQueryLemmas = Lemmatizer.getLemmasList(searchQueryText)
                .keySet()
                .stream()
                .map(l -> getLemma(l, site))
                .filter(Objects::nonNull)
                .filter(l -> isLemmasFrequencyNotTooHigh(l, pages.size()))
                .sorted(Comparator.comparing(Lemma::getFrequency))
                .toList();

        List<SearchResult> searchResultList = new LinkedList<>();

        if(!searchQueryLemmas.isEmpty()) {

            for (Lemma lemma : searchQueryLemmas) {
                pages.removeIf(page -> !page.getLemmasList().contains(lemma));
            }

            if (!pages.isEmpty()) {
                searchResultList = pages.stream()
                        .map(page ->
                                new SearchResult(page.getPath(),
                                        getPageTitle(page.getContent()),
                                        getSearchResultSnippet(page.getContent(), searchQueryLemmas),
                                        getPageRelevance(page, searchQueryLemmas),
                                        page.getSite())
                        )
                        .sorted(Comparator.comparing(SearchResult::getRelevance).reversed())
                        .toList();
            }
        }
        return searchResultList;
    }

    private String getPageTitle(String html) {
        return Jsoup.parse(html).title();
    }

    private String getSearchResultSnippet(String html, List<Lemma> searchQueryLemmas) {

        final String RUSSIAN_WORD_REGEX = "[А-Яа-я0-9]+";
        final String PUNCTUATION_REGEX = "[\\p{Punct}\\s]{1,3}";

        List<String> lemmas = new LinkedList<>();
        for(Lemma lemma : searchQueryLemmas) {
            lemmas.add(lemma.getLemma());
        }

        String searchQuery = searchQueryLemmas.get(0).getLemma();

        String snippet = "";

        String text = getPageText(html);
        String clearText = text.replaceAll("[^А-Яа-я\\s]", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String[] words = clearText.split("\\s");

        boolean isWordNotFind = true;
        int i = 0;
        LuceneMorphology luceneMorphology = Lemmatizer.getLuceneMorphology();
        String snippetMainWord = "";

        while(isWordNotFind) {
            String word = words[i];
            i++;
            if(luceneMorphology.getNormalForms(word).contains(searchQuery)){
                snippetMainWord = word;
                isWordNotFind = false;
            }
        }

        String SNIPPET_REGEX = "(" + RUSSIAN_WORD_REGEX + PUNCTUATION_REGEX + "){0,15}" + "\\b" + snippetMainWord + "\\b" +
                PUNCTUATION_REGEX + "(" + RUSSIAN_WORD_REGEX + PUNCTUATION_REGEX + "){0,15}";

        Pattern pattern = Pattern.compile(SNIPPET_REGEX);
        Matcher matcher = pattern.matcher(text);
        if(matcher.find()){
            snippet = matcher.group();
        }

        String clearSnippet = snippet.replaceAll("[^А-Яа-я\\s]", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String[] snippetWords = clearSnippet.split("\\s");

        boolean isWordContainsInSearchQuery = false;

        for(String word : snippetWords) {
            if(!word.isEmpty()) {
                List<String> normalForms = luceneMorphology.getNormalForms(word);
                for(String form : normalForms) {
                    if(lemmas.contains(form)) {
                        isWordContainsInSearchQuery = true;
                        break;
                    }
                }
                if(isWordContainsInSearchQuery) {
                    snippet = snippet.replaceAll(word, "<b>" + word + "</b>");
                    isWordContainsInSearchQuery = false;
                }
            }
        }
        return snippet.replaceAll("(<b>)+", "<b>").replaceAll("(</b>)+", "</b>");
    }

    private float getPageRelevance(Page page, List<Lemma> searchQueryLemmas) {
        return page.getLemmasList().stream()
                .filter(searchQueryLemmas::contains)
                .map(lemma -> getLemmaRankOnPage(page, lemma))
                .reduce(Float::sum)
                .get();
    }

    private String getPageText(String html) {
        return Jsoup.parse(html).body().text();
    }

    private float getLemmaRankOnPage(Page page, Lemma lemma){
        return indexRepository.findIndexByPageAndLemma(page, lemma).getRank();
    }

    private Lemma getLemma(String lemma, Site site) {
        return lemmaRepository.findLemmaByLemmaAndSite(lemma, site);
    }

    private boolean isLemmasFrequencyNotTooHigh(Lemma lemma, int pagesCount) {
        return lemma.getFrequency() < pagesCount * (MAX_FREQUENCY_IN_PERCENTS / 100);
    }

}
