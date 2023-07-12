package com.kvaster.iptv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Base64;
import java.util.Objects;

import com.kvaster.iptv.config.IptvConnectionConfig;
import com.kvaster.iptv.config.IptvServerConfig;

public class IptvServer {
    public static final String PROXY_USER_HEADER = "iptv-proxy-user";

    private final IptvServerConfig sc;
    private final IptvConnectionConfig cc;

    private final HttpClient httpClient;

    private int acquired;

    public IptvServer(IptvServerConfig sc, IptvConnectionConfig cc, HttpClient httpClient) {
        this.sc = Objects.requireNonNull(sc);
        this.cc = Objects.requireNonNull(cc);
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public String getName() {
        return sc.getName();
    }

    public String getUrl() {
        return cc.getUrl();
    }

    public String getServerPassword() {
        return cc.getPassword();
    }

    public boolean getSendUser() {
        return sc.getSendUser();
    }

    public boolean getSortChannels() {
        return sc.getSortChannels();
    }

    public String getChannelPrefix() {
        return sc.getChannelPrefix();
    }

    public boolean getProxyStream() {
        return sc.getProxyStream();
    }

    public long getChannelFailedMs() {
        return sc.getChannelFailedMs();
    }

    public long getInfoTimeoutMs() {
        return sc.getInfoTimeoutMs();
    }

    public long getInfoTotalTimeoutMs() {
        return sc.getInfoTotalTimeoutMs();
    }

    public long getInfoRetryDelayMs() {
        return sc.getInfoRetryDelayMs();
    }

    public long getCatchupTimeoutMs() {
        return sc.getCatchupTimeoutMs();
    }

    public long getCatchupTotalTimeoutMs() {
        return sc.getCatchupTotalTimeoutMs();
    }

    public long getCatchupRetryDelayMs() {
        return sc.getCatchupRetryDelayMs();
    }

    public long getStreamStartTimeoutMs() {
        return sc.getStreamStartTimeoutMs();
    }

    public long getStreamReadTimeoutMs() {
        return sc.getStreamReadTimeoutMs();
    }

    public synchronized boolean acquire() {
        if (acquired >= cc.getMaxConnections()) {
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

    public HttpRequest.Builder createRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        // add basic authentication
        if (cc.getLogin() != null && cc.getPassword() != null) {
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((cc.getLogin() + ":" + cc.getPassword()).getBytes()));
        }

        return builder;
    }
}
