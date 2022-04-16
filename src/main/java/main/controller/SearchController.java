package main.controller;

import main.YAMLConfig;
import main.model.Field;
import main.model.Site;
import main.model.SiteStatus;
import main.repositories.FieldRepository;
import main.repositories.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Default controller for Search Engine
 */

@Controller
public class SearchController {

    @Autowired
    private YAMLConfig config;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private FieldRepository fieldRepository;

    /**
     * Use webinterfacepath according to application.yml
     * @return html template
     */

    @GetMapping("/${webinterfacepath}")
    private String getInitialPage() {
        if(siteRepository.findAll().isEmpty()) {
            updateSiteList();
        }
        if(fieldRepository.findAll().isEmpty()) {
            updateFieldTable();
        }
        return "index";
    }

    /**
     * Update site table according to application.yml
     */

    @Transactional
    private void updateSiteList() {
        for(Map<String, String> site : config.getSites()) {
            Site s = new Site(site.get("url"), site.get("name"));
            s.setStatus(SiteStatus.NOTINDEXED);
            siteRepository.save(s);
        }
    }

    /**
     * Insert fields' values in to field table
     */

    @Transactional
    private void updateFieldTable() {
        Field title  = new Field("title", "title", 1.0f);
        Field body = new Field("body", "body", 0.8f);
        fieldRepository.save(title);
        fieldRepository.save(body);
    }

}
