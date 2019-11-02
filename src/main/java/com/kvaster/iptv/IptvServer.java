package com.kvaster.iptv;

public class IptvServer {
    private final String name;
    private final String url;
    private final int maxConnections;

    private int acquired;

    public IptvServer(String name, String url, int maxConnections) {
        this.name = name;
        this.url = url;
        this.maxConnections = maxConnections;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
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
