package com.kvaster.iptv.config;

import java.util.ArrayList;
import java.util.Collection;
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
    private boolean sortChannels = true;
    private Set<String> users = new HashSet<>();
    private long channelsTimeoutSec = 5;
    private long channelsTotalTimeoutSec = 60;
    private long channelsRetryDelayMs = 1000;
    private long xmltvTimeoutSec = 30;
    private long xmltvTotalTimeoutSec = 120;
    private long xmltvRetryDelayMs = 1000;
    private boolean useHttp2 = false;

    protected IptvProxyConfig() {
        // for deserialization
    }

    public IptvProxyConfig(
            String host, int port, String baseUrl, String forwardedPass,
            String tokenSalt, List<IptvServerConfig> servers,
            boolean allowAnonymous, boolean sortChannels, Set<String> users,
            int channelsTimeoutSec, int channelsTotalTimeoutSec, long channelsRetryDelayMs
    ) {
        this.host = host;
        this.port = port;
        this.baseUrl = baseUrl;
        this.forwardedPass = forwardedPass;
        this.tokenSalt = tokenSalt;
        this.servers = new ArrayList<>(servers);
        this.allowAnonymous = allowAnonymous;
        this.sortChannels = sortChannels;
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

    public boolean getUseHttp2() {
        return useHttp2;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final IptvProxyConfig c = new IptvProxyConfig();

        public IptvProxyConfig build() {
            return c;
        }

        public Builder host(String host) {
            c.host = host;
            return this;
        }

        public Builder port(int port) {
            c.port = port;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            c.baseUrl = baseUrl;
            return this;
        }

        public Builder forwardedPass(String forwardedPass) {
            c.forwardedPass = forwardedPass;
            return this;
        }

        public Builder tokenSalt(String tokenSalt) {
            c.tokenSalt = tokenSalt;
            return this;
        }

        public Builder servers(Collection<IptvServerConfig> servers) {
            c.servers = new ArrayList<>(servers);
            return this;
        }

        public Builder allowAnonymous(boolean allowAnonymous) {
            c.allowAnonymous = allowAnonymous;
            return this;
        }

        public Builder users(Collection<String> users) {
            c.users = new HashSet<>(users);
            return this;
        }

        public Builder channelsTimeoutSec(long channelsTimeoutSec) {
            c.channelsTimeoutSec = channelsTimeoutSec;
            return this;
        }

        public Builder channelsTotalTimeoutSec(long channelsTotalTimeoutSec) {
            c.channelsTotalTimeoutSec = channelsTotalTimeoutSec;
            return this;
        }

        public Builder channelsRetryDelayMs(long channelsRetryDelayMs) {
            c.channelsRetryDelayMs = channelsRetryDelayMs;
            return this;
        }

        public Builder xmltvTimeoutSec(long xmltvTimeoutSec) {
            c.xmltvTimeoutSec = xmltvTimeoutSec;
            return this;
        }

        public Builder xmltvTotalTimeoutSec(long xmltvTotalTimeoutSec) {
            c.xmltvTotalTimeoutSec = xmltvTotalTimeoutSec;
            return this;
        }

        public Builder xmltvRetryDelayMs(long xmltvRetryDelayMs) {
            c.xmltvRetryDelayMs = xmltvRetryDelayMs;
            return this;
        }

        public Builder useHttp2(boolean useHttp2) {
            c.useHttp2 = useHttp2;
            return this;
        }
    }

    public boolean getSortChannels() {
        return sortChannels;
    }
}
