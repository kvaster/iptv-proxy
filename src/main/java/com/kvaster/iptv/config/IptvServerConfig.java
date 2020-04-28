package com.kvaster.iptv.config;

public class IptvServerConfig {
    private String name;
    private String url;
    private int maxConnections;
    private boolean sendUser;
    private boolean proxyStream = true;
    private long channelFailedMs;
    private long retryTimeoutSec = 3;
    private long retryDelayMs = 500;

    protected IptvServerConfig() {
        // for deserialization
    }

    public IptvServerConfig(
            String name, String url, int maxConnections,
            boolean sendUser, boolean proxyStream,
            long channelFailedMs,
            long retryTimeoutSec,
            long retryDelayMs) {
        this.name = name;
        this.url = url;
        this.maxConnections = maxConnections;
        this.sendUser = sendUser;
        this.proxyStream = proxyStream;
        this.channelFailedMs = channelFailedMs;
        this.retryTimeoutSec = retryTimeoutSec;
        this.retryDelayMs = retryDelayMs;
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

    public long getRetryTimeoutSec() {
        return retryTimeoutSec;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }
}
