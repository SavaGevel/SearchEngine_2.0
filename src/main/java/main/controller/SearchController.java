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

@Controller
public class SearchController {

    @Autowired
    private YAMLConfig config;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private FieldRepository fieldRepository;


    @GetMapping("/")
    private String getInitialPage() {
        if(siteRepository.findAll().isEmpty()) {
            updateSiteList();
        }
        if(fieldRepository.findAll().isEmpty()) {
            updateFieldTable();
        }
        return "index";
    }

    @Transactional
    private void updateSiteList() {
        for(Map<String, String> site : config.getSites()) {
            Site s = new Site(site.get("url"), site.get("name"));
            s.setStatus(SiteStatus.NOTINDEXED);
            siteRepository.save(s);
        }
    }

    @Transactional
    private void updateFieldTable() {
        Field title  = new Field("title", "title", 1.0f);
        Field body = new Field("body", "body", 0.8f);
        fieldRepository.save(title);
        fieldRepository.save(body);
    }

}
