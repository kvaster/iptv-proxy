package com.kvaster.iptv.config;

import java.util.ArrayList;
import java.util.List;

public class IptvProxyConfig {
    private String host = "127.0.0.1";
    private int port = 8080;
    private String baseUrl;
    private int clientTimeoutSec = 10;
    private String tokenSalt;
    private List<IptvServerConfig> servers;

    protected IptvProxyConfig() {
        // for deserialization
    }

    public IptvProxyConfig(
            String host, int port, String baseUrl, int clientTimeoutSec, String tokenSalt,
            List<IptvServerConfig> servers
    ) {
        this.host = host;
        this.port = port;
        this.baseUrl = baseUrl;
        this.clientTimeoutSec = clientTimeoutSec;
        this.servers = new ArrayList<>(servers);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getClientTimeoutSec() {
        return clientTimeoutSec;
    }

    public String getTokenSalt() {
        return tokenSalt;
    }

    public List<IptvServerConfig> getServers() {
        return servers;
    }
}
