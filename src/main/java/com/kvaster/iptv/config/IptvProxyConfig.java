package com.kvaster.iptv.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IptvProxyConfig {
    private String host = "127.0.0.1";
    private int port = 8080;
    private String baseUrl;
    private String forwardedPass;
    private String tokenSalt;
    private List<IptvServerConfig> servers;
    private boolean allowAnonymous = true;
    private Set<String> users = new HashSet<>();
    private long channelsTimeoutSec = 5;
    private long channelsTotalTimeoutSec = 60;
    private long channelsRetryDelayMs = 1000;
    private long xmltvTimeoutSec = 30;
    private long xmltvTotalTimeoutSec = 120;
    private long xmltvRetryDelayMs = 1000;

    protected IptvProxyConfig() {
        // for deserialization
    }

    public IptvProxyConfig(
            String host, int port, String baseUrl, String forwardedPass,
            String tokenSalt, List<IptvServerConfig> servers,
            boolean allowAnonymous, Set<String> users,
            int channelsTimeoutSec, int channelsTotalTimeoutSec, long channelsRetryDelayMs
    ) {
        this.host = host;
        this.port = port;
        this.baseUrl = baseUrl;
        this.forwardedPass = forwardedPass;
        this.tokenSalt = tokenSalt;
        this.servers = new ArrayList<>(servers);
        this.allowAnonymous = allowAnonymous;
        this.users = new HashSet<>(users);
        this.channelsTimeoutSec = channelsTimeoutSec;
        this.channelsTotalTimeoutSec = channelsTotalTimeoutSec;
        this.channelsRetryDelayMs = channelsRetryDelayMs;
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

    public String getForwardedPass() {
        return forwardedPass;
    }

    public String getTokenSalt() {
        return tokenSalt;
    }

    public List<IptvServerConfig> getServers() {
        return servers;
    }

    public boolean getAllowAnonymous() {
        return allowAnonymous;
    }

    public Set<String> getUsers() {
        return users;
    }

    public long getChannelsTimeoutSec() {
        return channelsTimeoutSec;
    }

    public long getChannelsTotalTimeoutSec() {
        return channelsTotalTimeoutSec;
    }

    public long getChannelsRetryDelayMs() {
        return channelsRetryDelayMs;
    }

    public long getXmltvTimeoutSec() {
        return xmltvTimeoutSec;
    }

    public long getXmltvTotalTimeoutSec() {
        return xmltvTotalTimeoutSec;
    }

    public long getXmltvRetryDelayMs() {
        return xmltvRetryDelayMs;
    }
}
