package main.controller;

import main.YAMLConfig;
import main.index.PageIndexer;
import main.index.SiteBypassAction;
import main.model.Lemma;
import main.model.Page;
import main.model.Site;
import main.model.SiteStatus;
import main.repositories.*;
import main.search.Search;
import main.search.SearchResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


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


    @GetMapping("/statistics")
    private JSONObject getStatistics() {
        JSONObject response = new JSONObject();
        JSONObject statistics = new JSONObject();

        statistics.put("total", getTotalStatistics());
        statistics.put("detailed", getDetailedStatistics());

        response.put("result", true);
        response.put("statistics", statistics);

        return response;
    }

    @PostMapping("/indexPage")
    public JSONObject indexPage(@RequestParam String url) {
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
                return response;
            }
        }
        response.put("result", false);
        response.put("error", "страница вне выбранных сайтов");
        return response;
    }

    @GetMapping("/startIndexing")
    public JSONObject startIndexing() {
        JSONObject response = new JSONObject();

        for(Site site : siteRepository.findAll()) {
            if(site.getStatus().equals(SiteStatus.INDEXING)) {
                response.put("result", false);
                response.put("error", "Индексация уже запущена");
                return response;
            }
        }

        clearSiteTables();
        updateSiteList();
        startSiteIndexing();
        response.put("result", true);
        return response;
    }

    @GetMapping("/stopIndexing")
    public JSONObject stopIndexing() {
        JSONObject response = new JSONObject();
        for (Site site : siteRepository.findAll()) {
            site.setStatus(SiteStatus.FAILED);
            siteRepository.save(site);
        }
        ThreadPoolTaskScheduler f = new ThreadPoolTaskScheduler();

        response.put("result", true);
        return response;
    }

    @GetMapping("/search")
    public JSONObject search(@RequestParam String query, @RequestParam String site) {

        JSONObject response = new JSONObject();
        JSONArray searchResultList = new JSONArray();

        if(query.isEmpty()) {
            response.put("result", false);
            response.put("error", "Задан пустой поисковый запрос");
            return response;
        }

        if(siteRepository.findSiteByUrl(site).getStatus().equals(SiteStatus.INDEXING)) {
            response.put("result", false);
            response.put("error", "Происходит индексация сайта");
            return response;
        }

        Site s = siteRepository.findSiteByUrl(site);
        List<SearchResult> results = search.searching(query, s);

        if(results.isEmpty()) {
            response.put("result", false);
            response.put("error", "Не удалось найти страницы по данному запросу");
            response.put("Status-Code", 404);
            return response;
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

        return response;

    }

    private JSONObject getTotalStatistics() {
        JSONObject total = new JSONObject();
        total.put("sites", siteRepository.count());
        total.put("pages", pageRepository.count());
        total.put("lemmas", lemmaRepository.count());
        total.put("isIndexing", getIndexStatus());
        return total;
    }

    private boolean getIndexStatus() {
        for(Site site : siteRepository.findAll()) {
            if(site.getStatus().equals(SiteStatus.INDEXING)) {
                return true;
            }
        }
        return false;
    }

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

    private void clearSiteTables() {
        for(Site site : siteRepository.findAll()) {
            siteRepository.delete(site);
        }
    }

    @Async
    private void startSiteIndexing() {
        for(Site site : siteRepository.findAll()) {
            site.setStatus(SiteStatus.INDEXING);
            siteRepository.save(site);
            new SiteBypassAction(config, siteRepository, pageRepository, lemmaRepository, indexRepository, fieldRepository, "/", site).fork();
        }
    }

    @Transactional
    private void setSiteStatusToIndexed() {
        for(Site site : siteRepository.findAll()) {
            site.setStatus(SiteStatus.INDEXED);
            siteRepository.save(site);
        }
    }

    @Transactional
    private void updateSiteList() {
        for(Map<String, String> site : config.getSites()) {
            Site s = new Site(site.get("url"), site.get("name"));
            s.setStatus(SiteStatus.NOTINDEXED);
            siteRepository.save(s);
        }
    }

    private void startOnePageIndexing(Page page) {
        new Thread(new PageIndexer(config, pageRepository, lemmaRepository, indexRepository, fieldRepository, siteRepository, page)).start();
    }

}
