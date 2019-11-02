package com.kvaster.iptv;

public class ProxyConfig {
    private String host = "127.0.0.1";
    private int port = 8080;

    private String playlistUrl;
    private String baseUrl;

    protected ProxyConfig() {
        // for deserialization
    }

    public ProxyConfig(String host, int port, String playlistUrl, String baseUrl) {
        this.host = host;
        this.port = port;
        this.playlistUrl = playlistUrl;
        this.baseUrl = baseUrl;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPlaylistUrl() {
        return playlistUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
