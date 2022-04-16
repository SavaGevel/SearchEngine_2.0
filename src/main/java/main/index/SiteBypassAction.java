package main.index;

import main.YAMLConfig;
import main.model.Page;
import main.model.Site;
import main.model.SiteStatus;
import main.repositories.*;
import org.jsoup.Jsoup;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 * Action that bypasses all site's pages and start indexing them in different threads
 */

public class SiteBypassAction extends RecursiveAction {

    private final YAMLConfig config;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final FieldRepository fieldRepository;

    private String url;
    private Site site;

    public SiteBypassAction(YAMLConfig config, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, FieldRepository fieldRepository, String url, Site site) {
        this.config = config;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.fieldRepository = fieldRepository;
        this.url = url;
        this.site = site;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    /**
     * Checks site status
     * Check is page was already visited. If not, begin page indexing.
     * Create new action for every link on that page
     */

    @Override
    public void compute() {

        if (!siteRepository.findSiteByUrl(site.getUrl()).getStatus().equals(SiteStatus.FAILED)) {

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (isNotVisited()) {
                Page page = new Page(url, site);
                new Thread(new PageIndexer(config, pageRepository, lemmaRepository, indexRepository, fieldRepository, siteRepository, page)).start();
                for (String path : getLinksOnPage()) {
                    new SiteBypassAction(config, siteRepository, pageRepository, lemmaRepository, indexRepository, fieldRepository, path, site).invoke();
                }
            }
        }
    }

    /**
     * Checks if page was already visited
     * @return true if page is not visited
     */

    private boolean isNotVisited() {
        return pageRepository.findPageByPathAndSite(url, site) == null;
    }

    /**
     * Gets all links on page and filter them
     * @return list of links that lead only to site's pages
     */

    private List<String> getLinksOnPage() {
        try {
            return Jsoup.connect(site.getUrl().concat(url)).userAgent(config.getUserAgent()).referrer(config.getReferrer()).get()
                    .body()
                    .select("a[href]").stream()
                    .map(element -> element.attr("href"))
                    .filter(link -> link.indexOf("/") == 0)
                    .toList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }


}
