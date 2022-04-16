package main.search;

import main.model.Site;

/**
 * Entity that describes structure of search result
 */

public class SearchResult {


    private String uri;
    private String title;
    private String snippet;
    private float relevance;
    private Site site;

    public SearchResult(String uri, String title, String snippet, float relevance, Site site) {
        this.uri = uri;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
        this.site = site;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public float getRelevance() {
        return relevance;
    }

    public void setRelevance(float relevance) {
        this.relevance = relevance;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }
}