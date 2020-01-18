package com.kvaster.iptv.config;

public class IptvServerConfig {
    private String name;
    private String url;
    private int maxConnections;
    private boolean sendUser;

    protected IptvServerConfig() {
        // for deserialization
    }

    public IptvServerConfig(String name, String url, int maxConnections, boolean sendUser) {
        this.url = url;
        this.maxConnections = maxConnections;
        this.sendUser = sendUser;
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

    public boolean getSendUser() {
        return sendUser;
    }
}
