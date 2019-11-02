package com.kvaster.iptv.config;

public class IptvServerConfig {
    private String name;
    private String url;
    private int maxConnections;

    protected IptvServerConfig() {
        // for deserialization
    }

    public IptvServerConfig(String name, String url, int maxConnections) {
        this.url = url;
        this.maxConnections = maxConnections;
    }

    public String getName() {
        return name == null ? url : name;
    }

    public String getUrl() {
        return url;
    }

    public int getMaxConnections() {
        return maxConnections;
    }
}
