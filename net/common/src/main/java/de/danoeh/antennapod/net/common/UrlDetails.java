package de.danoeh.antennapod.net.common;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlDetails {
    private final String source;
    private URL url;

    public UrlDetails(String source) {
        this.source = source;
        initializeURL();
    }

    private void initializeURL() {
        try {
            this.url = new URL(source);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + source, e);
        }
    }

    public String getProtocol() {
        return url.getProtocol();
    }

    public String getUserInfo() {
        return url.getUserInfo();
    }

    public String getHost() {
        return url.getHost();
    }

    public int getPort() {
        return url.getPort();
    }

    public String getPath() {
        return url.getPath();
    }

    public String getQuery() {
        return url.getQuery();
    }

    public String getRef() {
        return url.getRef();
    }
}