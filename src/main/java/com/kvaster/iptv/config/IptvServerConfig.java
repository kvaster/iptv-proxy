package com.kvaster.iptv.config;

public class IptvServerConfig {
    private String name;
    private String url;
    private int maxConnections;
    private boolean sendUser;
    private boolean proxyStream = true;
    private long channelFailedMs;
    private long infoTimeoutSec = 3;
    private long infoRetryDelayMs = 500;

    protected IptvServerConfig() {
        // for deserialization
    }

    public IptvServerConfig(
            String name, String url, int maxConnections,
            boolean sendUser, boolean proxyStream,
            long channelFailedMs,
            long infoTimeoutSec,
            long infoRetryDelayMs
    ) {
        this.name = name;
        this.url = url;
        this.maxConnections = maxConnections;
        this.sendUser = sendUser;
        this.proxyStream = proxyStream;
        this.channelFailedMs = channelFailedMs;
        this.infoTimeoutSec = infoTimeoutSec;
        this.infoRetryDelayMs = infoRetryDelayMs;
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

    public long getChannelFailedMs() {
        return channelFailedMs;
    }

    public long getInfoTimeoutSec() {
        return infoTimeoutSec;
    }

    public long getInfoRetryDelayMs() {
        return infoRetryDelayMs;
    }
}
