package com.kvaster.iptv;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

import com.kvaster.iptv.config.IptvConnectionConfig;
import com.kvaster.iptv.config.IptvServerConfig;

public class IptvServer {
    public static final String PROXY_USER_HEADER = "iptv-proxy-user";

    private final IptvServerConfig sc;
    private final IptvConnectionConfig cc;

    private final HttpClient httpClient;

    private int acquired;

    public IptvServer(IptvServerConfig sc, IptvConnectionConfig cc) {
        this.sc = Objects.requireNonNull(sc);
        this.cc = Objects.requireNonNull(cc);

        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
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

    public String getServerUser() {
        return cc.getUser();
    }

    public String getServerPassword() {
        return cc.getPassword();
    }

    public boolean getSendUser() {
        return sc.getSendUser();
    }

    public boolean getProxyStream() {
        return sc.getProxyStream();
    }

    public long getChannelFailedMs() {
        return sc.getChannelFailedMs();
    }

    public long getInfoTimeoutSec() {
        return sc.getInfoTimeoutSec();
    }

    public long getInfoTotalTimeoutSec() {
        return sc.getInfoTotalTimeoutSec();
    }

    public long getInfoRetryDelayMs() {
        return sc.getInfoRetryDelayMs();
    }

    public long getCatchupTimeoutSec() {
        return sc.getCatchupTimeoutSec();
    }

    public long getCatchupTotalTimeoutSec() {
        return sc.getCatchupTotalTimeoutSec();
    }

    public long getCatchupRetryDelayMs() {
        return sc.getCatchupRetryDelayMs();
    }

    public long getStreamStartTimeoutSec() {
        return sc.getStreamStartTimeoutSec();
    }

    public long getStreamReadTimeoutSec() {
        return sc.getStreamReadTimeoutSec();
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
}
