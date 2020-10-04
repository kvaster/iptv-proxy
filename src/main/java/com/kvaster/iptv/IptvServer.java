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
    private final long infoTotalTimeoutSec;
    private final long infoRetryDelayMs;
    private final long catchupTimeoutSec;
    private final long catchupTotalTimeoutSec;
    private final long catchupRetryDelayMs;
    private final long streamConnectTimeoutSec;

    private final HttpClient httpClient;

    private int acquired;

    public IptvServer(IptvServerConfig sc) {
        this(
                sc.getName(), sc.getUrl(), sc.getMaxConnections(),
                sc.getSendUser(), sc.getProxyStream(), sc.getChannelFailedMs(),
                sc.getInfoTimeoutSec(), sc.getInfoTotalTimeoutSec(), sc.getInfoRetryDelayMs(),
                sc.getCatchupTimeoutSec(), sc.getCatchupTotalTimeoutSec(), sc.getCatchupRetryDelayMs(),
                sc.getStreamConnectTimeoutSec()
        );
    }

    public IptvServer(String name, String url, int maxConnections,
            boolean sendUser, boolean proxyStream,
            long channelFailedMs,
            long infoTimeoutSec, long infoTotalTimeoutSec, long infoRetryDelayMs,
            long catchupTimeoutSec, long catchupTotalTimeoutSec, long catchupRetryDelayMs,
            long streamConnectTimeoutSec
    ) {
        this.name = name;
        this.url = url;
        this.maxConnections = maxConnections;
        this.sendUser = sendUser;
        this.proxyStream = proxyStream;
        this.channelFailedMs = channelFailedMs;
        this.infoTimeoutSec = infoTimeoutSec;
        this.infoTotalTimeoutSec = infoTotalTimeoutSec;
        this.infoRetryDelayMs = infoRetryDelayMs;
        this.catchupTimeoutSec = catchupTimeoutSec;
        this.catchupTotalTimeoutSec = catchupTotalTimeoutSec;
        this.catchupRetryDelayMs = catchupRetryDelayMs;
        this.streamConnectTimeoutSec = streamConnectTimeoutSec;

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

    public long getInfoTotalTimeoutSec() {
        return infoTotalTimeoutSec;
    }

    public long getInfoRetryDelayMs() {
        return infoRetryDelayMs;
    }

    public long getCatchupTimeoutSec() {
        return catchupTimeoutSec;
    }

    public long getCatchupTotalTimeoutSec() {
        return catchupTotalTimeoutSec;
    }

    public long getCatchupRetryDelayMs() {
        return catchupRetryDelayMs;
    }

    public long getStreamConnectTimeoutSec() {
        return streamConnectTimeoutSec;
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
