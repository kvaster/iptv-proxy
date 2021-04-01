package com.kvaster.iptv.config;

public class IptvConnectionConfig {
    private String url;
    private int maxConnections;
    private String login;
    private String password;

    protected IptvConnectionConfig() {
        // for deserialization
    }

    public IptvConnectionConfig(String url, int maxConnections) {
        this.url = url;
        this.maxConnections = maxConnections;
    }

    public String getUrl() {
        return url;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
