package main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
public class YAMLConfig {

    private final String DEFAULT_WEB_INTERFACE_PATH = "/";

    private List<Map<String, String>> sites = new LinkedList<>();
    private String userAgent;
    private String referrer;
    private String webInterfacePath;

    public List<Map<String, String>> getSites() {
        return sites;
    }

    public void setSites(List<Map<String, String>> sites) {
        this.sites = sites;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getWebInterfacePath() {
        return webInterfacePath == null ? DEFAULT_WEB_INTERFACE_PATH : webInterfacePath;
    }

    public void setWebInterfacePath(String webInterfacePath) {
        this.webInterfacePath = webInterfacePath;
    }
}
