package main.controller;

import main.YAMLConfig;
import main.index.PageIndexer;
import main.index.SiteBypassAction;
import main.model.Page;
import main.model.Site;
import main.model.SiteStatus;
import main.repositories.*;
import main.search.Search;
import main.search.SearchResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Rest controller for processing requests
 */

@RestController
public class SearchRestController {

    @Autowired
    private YAMLConfig config;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexRepository indexRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private Search search;

    /**
     * Get statistics about sites
     * @return all statistics about sites
     */

    @GetMapping("/statistics")
    private ResponseEntity<JSONObject> getStatistics() {
        JSONObject response = new JSONObject();
        JSONObject statistics = new JSONObject();

        statistics.put("total", getTotalStatistics());
        statistics.put("detailed", getDetailedStatistics());

        response.put("result", true);
        response.put("statistics", statistics);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Check if url is a part of any site from site table
     * and start indexing if it's true
     * @param url This is url of page that should be added to index table
     * @return true if page was indexed successfully
     */

    @PostMapping("/indexPage")
    public ResponseEntity<JSONObject> indexPage(@RequestParam String url) {

        JSONObject response = new JSONObject();

        for(Site site : siteRepository.findAll()) {
            if(url.contains(site.getUrl())) {
                Page page = new Page(url.replaceAll(site.getUrl(), ""), site);
                Page p = pageRepository.findPageByPathAndSite(page.getPath(), site);
                if(p != null) {
                    pageRepository.delete(p);
                }
                startOnePageIndexing(page);
                response.put("result", true);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        }
        response.put("result", false);
        response.put("error", "страница вне выбранных сайтов");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Start page indexing in new thread
     * @param page This page should be indexed
     */

    private void startOnePageIndexing(Page page) {
        new Thread(new PageIndexer(config, pageRepository, lemmaRepository, indexRepository, fieldRepository, siteRepository, page)).start();
    }

    /**
     * Start indexing for all sites from site table
     * Clear db before indexing
     * Update list of sites according application.yml
     * Start indexing for every site in new thread
     * @return true if indexing was started successfully
     */

    @GetMapping("/startIndexing")
    public ResponseEntity<JSONObject> startIndexing() {
        JSONObject response = new JSONObject();

        if (isIndexing()) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        clearSiteTables();
        updateSiteList();
        startSiteIndexing();
        response.put("result", true);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Start indexing for every site in new thread
     */

    private void startSiteIndexing() {
        for(Site site : siteRepository.findAll()) {
            setSiteStatusToIndexing(site);
            new SiteBypassAction(config, siteRepository, pageRepository, lemmaRepository, indexRepository, fieldRepository, "/", site).fork();
        }
    }

    /**
     * Stop indexing for all sites from site table
     * @return HttpStatus.OK
     */

    @GetMapping("/stopIndexing")
    public ResponseEntity<JSONObject> stopIndexing() {
        JSONObject response = new JSONObject();
        stopSitesIndexing();
        response.put("result", true);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Set siteStatus to FAILED for every site and stops indexing
     */

    @Transactional
    private void stopSitesIndexing() {
        for (Site site : siteRepository.findAll()) {
            site.setStatus(SiteStatus.FAILED);
            siteRepository.save(site);
        }
        siteRepository.flush();
        pageRepository.flush();
        lemmaRepository.flush();
        indexRepository.flush();
    }

    /**
     * Search for pages according to query and chosen site
     * @param query This is a search query
     * @param site This site was chosen for search. If site is not chosen method will be search on all sites.
     * @param offset
     * @param limit This is the limit of list of search results
     * @return list of search results
     */

    @GetMapping("/search")
    public ResponseEntity<JSONObject> search(@RequestParam String query, @RequestParam(required = false) String site, @RequestParam(required = false) int offset, @RequestParam(required = false) int limit) {

        int DEFAULT_OFFSET = 0;
        int DEFAULT_LIMIT = 20;

        JSONObject response = new JSONObject();
        JSONArray searchResultList = new JSONArray();

        if(query.isEmpty()) {
            response.put("result", false);
            response.put("error", "Задан пустой поисковый запрос");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (isIndexing()) {
            response.put("result", false);
            response.put("error", "Происходит индексация сайта");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Site s = siteRepository.findSiteByUrl(site);
        List<SearchResult> results = search.searching(query, s).stream().limit(limit == 0 ? DEFAULT_LIMIT : limit).toList();

        if(results.isEmpty()) {
            response.put("result", false);
            response.put("error", "Не удалось найти страницы по данному запросу");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        for(SearchResult result : results) {
            JSONObject searchResult = new JSONObject();
            searchResult.put("site", result.getSite().getUrl());
            searchResult.put("siteName", result.getSite().getName());
            searchResult.put("uri", result.getUri());
            searchResult.put("title", result.getTitle());
            searchResult.put("snippet", result.getSnippet());
            searchResult.put("relevance", result.getRelevance());
            searchResultList.add(searchResult);
        }

        response.put("result", true);
        response.put("count", results.size());
        response.put("data", searchResultList);

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    /**
     * Check if indexing is not finished yet.
     * If site's statusTime was updated MAX_MINUTES_NOT_UPDATE_STATUS_TIME_FOR_SET_INDEXED minutes ago
     * site's status sets to INDEXED
     */

    @Scheduled(fixedRate = 5000)
    public void checkIsIndexingFinished() {

        long MAX_MINUTES_NOT_UPDATE_STATUS_TIME_FOR_SET_INDEXED = 5;

        for(Site site : siteRepository.findAll()) {

            boolean isIndexing = site.getStatus().equals(SiteStatus.INDEXING);

            LocalDateTime statusTime = site.getStatusTime();

            boolean isTooMuchTimePassedAfterLastStatusTimeUpdate =
                    statusTime != null && statusTime.until(LocalDateTime.now(), ChronoUnit.MINUTES) > MAX_MINUTES_NOT_UPDATE_STATUS_TIME_FOR_SET_INDEXED;

            if(isIndexing && isTooMuchTimePassedAfterLastStatusTimeUpdate) {
                setSiteStatusToIndexed(site);
            }
        }
    }

    /**
     * Get total statics for all sites from db
     * @return JSONObject with total statistics
     */

    private JSONObject getTotalStatistics() {
        JSONObject total = new JSONObject();
        total.put("sites", siteRepository.count());
        total.put("pages", pageRepository.count());
        total.put("lemmas", lemmaRepository.count());
        total.put("isIndexing", isIndexing());
        return total;
    }

    /**
     * Check if indexing is happening
     * @return true if at least one site is indexing now
     */

    private boolean isIndexing() {
        for(Site site : siteRepository.findAll()) {
            if(site.getStatus().equals(SiteStatus.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get detailed statistics for every site from db
     * @return JSONArray with detailed statistics
     */

    private JSONArray getDetailedStatistics() {

        JSONArray detailed = new JSONArray();

        for(Site site : siteRepository.findAll()) {
            JSONObject siteInfo = new JSONObject();
            siteInfo.put("url", site.getUrl());
            siteInfo.put("name", site.getName());
            siteInfo.put("status", site.getStatus());
            siteInfo.put("statusTime", site.getStatusTime());
            siteInfo.put("error", site.getLastError());
            siteInfo.put("pages", pageRepository.findPageBySite(site).size());
            siteInfo.put("lemmas", lemmaRepository.findLemmaBySite(site).size());

            detailed.add(siteInfo);
        }
        return detailed;
    }

    /**
     * Clear site table
     */

    private void clearSiteTables() {
        for(Site site : siteRepository.findAll()) {
            siteRepository.delete(site);
        }
    }

    /**
     * Set site status to INDEXING
     * @param site
     */

    @Transactional
    private void setSiteStatusToIndexing(Site site) {
        site.setStatus(SiteStatus.INDEXING);
        siteRepository.save(site);
    }

    /**
     * Set site status to INDEXED
     * @param site
     */

    @Transactional
    private void setSiteStatusToIndexed(Site site) {
        site.setStatus(SiteStatus.INDEXED);
        siteRepository.save(site);
    }

    /**
     * Update site list according to application.yml
     */

    @Transactional
    private void updateSiteList() {
        for(Map<String, String> site : config.getSites()) {
            Site s = new Site(site.get("url"), site.get("name"));
            s.setStatus(SiteStatus.NOTINDEXED);
            siteRepository.save(s);
        }
    }

}
