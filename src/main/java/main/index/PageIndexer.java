package main.index;

import main.YAMLConfig;
import main.lemmatizer.Lemmatizer;
import main.model.*;
import main.repositories.*;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PageIndexer implements Runnable{

    private final YAMLConfig config;

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final FieldRepository fieldRepository;
    private final SiteRepository siteRepository;

    private final Page page;

    public PageIndexer(YAMLConfig config, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, FieldRepository fieldRepository, SiteRepository siteRepository, Page page) {
        this.config = config;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.fieldRepository = fieldRepository;
        this.siteRepository = siteRepository;
        this.page = page;
    }

    @Override
    public void run() {

        updateStatusTime(page.getSite());

        try {
            page.setCode(getStatusCode());
            page.setContent(getPageContent());
        } catch (IOException e) {
            updateLastError(page.getSite(), e.getMessage());
            e.printStackTrace();
        }

        insertInToPageTable();

        if (page.getCode() == HttpsURLConnection.HTTP_OK) {
            Map<String, Integer> titleLemmasList = getHtmlTitleLemmasList(page.getContent());
            Map<String, Integer> bodyLemmasList = getHtmlBodyLemmasList(page.getContent());

            Set<String> lemmas = new HashSet<>();
            lemmas.addAll(titleLemmasList.keySet());
            lemmas.addAll(bodyLemmasList.keySet());
            insertInToLemmaTable(lemmas);
            insertInToIndexTable(lemmas, titleLemmasList, bodyLemmasList);
        }

    }

    @Transactional
    private void updateStatusTime(Site site) {
        Site s = siteRepository.findSiteByUrl(site.getUrl());
        s.setStatusTime(LocalDateTime.now());
        siteRepository.save(s);
    }

    @Transactional
    private void updateLastError(Site site, String error) {
        Site s = siteRepository.findSiteByUrl(site.getUrl());
        s.setLastError(error);
        siteRepository.save(s);
    }

    private Map<String, Integer> getHtmlTitleLemmasList(String pageContent) {
        return Lemmatizer.getLemmasList(Jsoup.parse(pageContent).title());
    }

    private Map<String, Integer> getHtmlBodyLemmasList(String pageContent) {
        return Lemmatizer.getLemmasList(Jsoup.parse(pageContent).body().text());
    }

    @Transactional
    private void insertInToPageTable() {
        if(pageRepository.findPageByPathAndSite(page.getPath(), page.getSite()) == null) {
            pageRepository.save(page);
        }
    }

    @Transactional
    private void insertInToLemmaTable(Set<String> lemmas) {
        for(String lemma : lemmas) {
            Lemma l = lemmaRepository.findLemmaByLemmaAndSite(lemma, page.getSite());
            if(l == null) {
                lemmaRepository.save(new Lemma(lemma, 1, page.getSite()));
            } else {
                l.setFrequency(l.getFrequency() + 1);
                lemmaRepository.save(l);
            }
        }
    }

    @Transactional
    private void insertInToIndexTable(Set<String> lemmas, Map<String, Integer> titleLemmasList, Map<String, Integer> bodyLemmasList) {
        float titleWeight = getWeightForLemmasInHtml("title");
        float bodyWeight = getWeightForLemmasInHtml("body");
        float rank;

        for(String lemma : lemmas) {
            Lemma l = lemmaRepository.findLemmaByLemmaAndSite(lemma, page.getSite());
            rank = titleLemmasList.getOrDefault(lemma, 0) * titleWeight
                    + bodyLemmasList.getOrDefault(lemma, 0) * bodyWeight;
            indexRepository.save(new Index(page, l, rank));
        }

    }


    private float getWeightForLemmasInHtml(String name) {
        return fieldRepository.getFieldByName(name).getWeight();
    }

    private int getStatusCode() throws IOException {
        return Jsoup.connect(page.getSite().getUrl().concat(page.getPath())).userAgent(config.getUserAgent()).referrer(config.getReferrer()).execute().statusCode();
    }


    private String getPageContent() throws IOException {
        return Jsoup.connect(page.getSite().getUrl().concat(page.getPath())).userAgent(config.getUserAgent()).referrer(config.getReferrer()).get().toString().replace("\"", "\\\"");
    }


}
