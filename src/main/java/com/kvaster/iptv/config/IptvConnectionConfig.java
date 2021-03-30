package com.kvaster.iptv.config;

public class IptvConnectionConfig {
    private String url;
    private int maxConnections;
    private String user;
    private String password;

    protected IptvConnectionConfig() {
        // for deserialization
    }

    public IptvConnectionConfig(String url, int maxConnections) {
        this.url = url;
        this.maxConnections = maxConnections;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public int getMaxConnections() {
        return maxConnections;
    }
}
