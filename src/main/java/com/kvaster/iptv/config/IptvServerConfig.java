package com.kvaster.iptv.config;

public class IptvServerConfig {
    private String name;
    private String url;
    private int maxConnections;
    private boolean sendUser;
    private boolean proxyStream = true;

    protected IptvServerConfig() {
        // for deserialization
    }

    public IptvServerConfig(String name, String url, int maxConnections, boolean sendUser, boolean proxyStream) {
        this.name = name;
        this.url = url;
        this.maxConnections = maxConnections;
        this.sendUser = sendUser;
        this.proxyStream = proxyStream;
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

    public boolean getProxyStream() {
        return proxyStream;
    }
}
