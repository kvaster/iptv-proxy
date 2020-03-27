package com.kvaster.iptv;

public class IptvServer {
    public static final String PROXY_USER_HEADER = "iptv-proxy-user";

    private final String name;
    private final String url;
    private final int maxConnections;
    private final boolean sendUser;
    private final boolean proxyStream;

    private int acquired;

    public IptvServer(String name, String url, int maxConnections, boolean sendUser, boolean proxyStream) {
        this.name = name;
        this.url = url;
        this.maxConnections = maxConnections;
        this.sendUser = sendUser;
        this.proxyStream = proxyStream;
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
