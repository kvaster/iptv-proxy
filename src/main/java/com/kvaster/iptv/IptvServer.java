package com.kvaster.iptv;

import java.net.http.HttpClient;

import com.kvaster.iptv.config.IptvServerConfig;

public class IptvServer {
    public static final String PROXY_USER_HEADER = "iptv-proxy-user";

    private final String name;
    private final String url;
    private final int maxConnections;
    private final boolean sendUser;
    private final boolean proxyStream;
    private final long channelFailedMs;
    private final long infoTimeoutSec;
    private final long infoRetryDelayMs;

    private final HttpClient httpClient;

    private int acquired;

    public IptvServer(IptvServerConfig sc) {
        this(
                sc.getName(), sc.getUrl(), sc.getMaxConnections(),
                sc.getSendUser(), sc.getProxyStream(), sc.getChannelFailedMs(),
                sc.getInfoTimeoutSec(), sc.getInfoRetryDelayMs()
        );
    }

    public IptvServer(String name, String url, int maxConnections,
            boolean sendUser, boolean proxyStream,
            long channelFailedMs,
            long infoTimeoutSec, long infoRetryDelayMs
    ) {
        this.name = name;
        this.url = url;
        this.maxConnections = maxConnections;
        this.sendUser = sendUser;
        this.proxyStream = proxyStream;
        this.channelFailedMs = channelFailedMs;
        this.infoTimeoutSec = infoTimeoutSec;
        this.infoRetryDelayMs = infoRetryDelayMs;

        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
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

    public synchronized boolean acquire() {
        if (acquired >= maxConnections) {
            return false;
        }

        acquired++;
        return true;
    }

    public synchronized void release() {
        if (acquired > 0) {
            acquired--;
        }
    }
}
