package main.index;

import main.YAMLConfig;
import main.lemmatizer.Lemmatizer;
import main.model.*;
import main.repositories.*;
import org.jsoup.Jsoup;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runnable class that index page and insert information in page, lemma and index tables
 */

public class PageIndexer implements Runnable {

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


    /**
     * Insert page to page table with status code and page content
     * Get lemmas from page and insert them in to lemma table
     * Create indexes for pages and lemmas and insert them in to index table
     */

    @Override
    public void run() {

        if (!siteRepository.findSiteByUrl(page.getSite().getUrl()).getStatus().equals(SiteStatus.FAILED)) {

            try {
                page.setCode(getStatusCode());
                page.setContent(getPageContent());
                insertInToPageTable(page);
                updateStatusTime(page.getSite());
            } catch (IOException e) {
                updateLastError(page.getSite(), e.getMessage());
                e.printStackTrace();
            }

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

    }

    /**
     * Update status time for site
     * @param site site that contains indexing page
     */

    @Transactional
    private void updateStatusTime(Site site) {
        Site s = siteRepository.findSiteByUrl(site.getUrl());
        s.setStatusTime(LocalDateTime.now());
        siteRepository.save(s);
    }

    /**
     * Update info about last error
     * @param site site where error was caught
     * @param error error message
     */

    @Transactional
    private void updateLastError(Site site, String error) {
        Site s = siteRepository.findSiteByUrl(site.getUrl());
        s.setLastError(error);
        siteRepository.save(s);
    }

    /**
     * Insert page in to page table
     * @param page page that should be inserted
     */

    @Transactional
    private void insertInToPageTable(Page page) {
        if (pageRepository.findPageByPathAndSite(page.getPath(), page.getSite()) == null) {
            pageRepository.save(page);
        }
    }

    /**
     * Insert lemmas in to lemma table
     * @param lemmas list of lemmas from page
     */

    @Transactional
    private synchronized void insertInToLemmaTable(Set<String> lemmas) {
        for (String lemma : lemmas) {
            Lemma l = lemmaRepository.findLemmaByLemmaAndSite(lemma, page.getSite());
            if (l == null) {
                lemmaRepository.save(new Lemma(lemma, 1, page.getSite()));
            } else {
                l.setFrequency(l.getFrequency() + 1);
                lemmaRepository.save(l);
            }
        }
    }

    /**
     * Insert indexes to index table
     * @param lemmas list of unique lemmas from page
     * @param titleLemmasList Map of lemmas with frequency that page title contains
     * @param bodyLemmasList Map of lemmas with frequency that page body contains
     */

    @Transactional
    private void insertInToIndexTable(Set<String> lemmas, Map<String, Integer> titleLemmasList, Map<String, Integer> bodyLemmasList) {
        float titleWeight = getWeightForLemmasInHtml("title");
        float bodyWeight = getWeightForLemmasInHtml("body");
        float rank;

        for (String lemma : lemmas) {
            synchronized (lemmaRepository) {
                Lemma l = lemmaRepository.findLemmaByLemmaAndSite(lemma, page.getSite());
                rank = titleLemmasList.getOrDefault(lemma, 0) * titleWeight
                        + bodyLemmasList.getOrDefault(lemma, 0) * bodyWeight;
                indexRepository.save(new Index(pageRepository.findPageByPathAndSite(page.getPath(), page.getSite()), l, rank));
            }
        }
    }

    /**
     * Get lemmas from page title
     * @param pageContent page html code
     * @return Map of lemmas with frequency from title
     */

    private Map<String, Integer> getHtmlTitleLemmasList(String pageContent) {
        return Lemmatizer.getLemmasList(Jsoup.parse(pageContent).title());
    }

    /**
     * Get lemmas from page title
     * @param pageContent page html code
     * @return Map of lemmas with frequency from body
     */

    private Map<String, Integer> getHtmlBodyLemmasList(String pageContent) {
        return Lemmatizer.getLemmasList(Jsoup.parse(pageContent).body().text());
    }

    /**
     * Get weight for lemma  from exact field according to value in field table
     * @param name field name
     * @return value of field weight
     */

    private float getWeightForLemmasInHtml(String name) {
        return fieldRepository.getFieldByName(name).getWeight();
    }

    /**
     * Get status code of page
     * @return http status code
     * @throws IOException on connect to page
     */

    private int getStatusCode() throws IOException {
        return Jsoup.connect(page.getSite().getUrl().concat(page.getPath())).userAgent(config.getUserAgent()).referrer(config.getReferrer()).execute().statusCode();
    }

    /**
     * Get html code
     * @return page html code
     * @throws IOException on connect to page
     */

    private String getPageContent() throws IOException {
        return Jsoup.connect(page.getSite().getUrl().concat(page.getPath())).userAgent(config.getUserAgent()).referrer(config.getReferrer()).get().toString().replace("\"", "\\\"");
    }


}
