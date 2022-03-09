package com.kvaster.iptv.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class IptvServerConfig {
    private String name;
    private List<IptvConnectionConfig> connections;
    private String xmltvUrl;
    private boolean sendUser;
    private boolean proxyStream = true;
    private long channelFailedMs;
    private long infoTimeoutMs = 1000;
    private long infoTotalTimeoutMs = 2000;
    private long infoRetryDelayMs = 100;
    private long catchupTimeoutMs = 1000;
    private long catchupTotalTimeoutMs = 2000;
    private long catchupRetryDelayMs = 100;
    private long streamStartTimeoutMs = 1000;
    private long streamReadTimeoutMs = 1000;

    private List<Pattern> groupFilters = Collections.emptyList();

    private IptvServerConfig() {
    }

    public String getName() {
        return name;
    }

    public List<IptvConnectionConfig> getConnections() {
        return connections;
    }

    public String getXmltvUrl() {
        return xmltvUrl;
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

    public long getInfoTimeoutMs() {
        return infoTimeoutMs;
    }

    public long getInfoTotalTimeoutMs() {
        return infoTotalTimeoutMs;
    }

    public long getInfoRetryDelayMs() {
        return infoRetryDelayMs;
    }

    public long getCatchupTimeoutMs() {
        return catchupTimeoutMs;
    }

    public long getCatchupTotalTimeoutMs() {
        return catchupTotalTimeoutMs;
    }

    public long getCatchupRetryDelayMs() {
        return catchupRetryDelayMs;
    }

    public long getStreamStartTimeoutMs() {
        return streamStartTimeoutMs;
    }

    public long getStreamReadTimeoutMs() {
        return streamReadTimeoutMs;
    }

    public List<Pattern> getGroupFilters() {
        return groupFilters;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final IptvServerConfig c = new IptvServerConfig();

        public IptvServerConfig build() {
            return c;
        }

        public Builder name(String name) {
            c.name = name;
            return this;
        }

        public Builder connections(Collection<IptvConnectionConfig> connections) {
            c.connections = new ArrayList<>(connections);
            return this;
        }

        public Builder xmltvUrl(String xmltvUrl) {
            c.xmltvUrl = xmltvUrl;
            return this;
        }

        public Builder sendUser(boolean sendUser) {
            c.sendUser = sendUser;
            return this;
        }

        public Builder proxyStream(boolean proxyStream) {
            c.proxyStream = proxyStream;
            return this;
        }

        public Builder channelFailedMs(long channelFailedMs) {
            c.channelFailedMs = channelFailedMs;
            return this;
        }

        public Builder infoTimeoutMs(long infoTimeoutMs) {
            c.infoTimeoutMs = infoTimeoutMs;
            return this;
        }

        public Builder infoTotalTimeoutMs(long infoTotalTimeoutMs) {
            c.infoTotalTimeoutMs = infoTotalTimeoutMs;
            return this;
        }

        public Builder infoRetryDelayMs(long infoRetryDelayMs) {
            c.infoRetryDelayMs = infoRetryDelayMs;
            return this;
        }

        public Builder catchupTimeoutMs(long catchupTimeoutMs) {
            c.catchupTimeoutMs = catchupTimeoutMs;
            return this;
        }

        public Builder catchupTotalTimeoutMs(long catchupTotalTimeoutMs) {
            c.catchupTotalTimeoutMs = catchupTotalTimeoutMs;
            return this;
        }

        public Builder catchupRetryDelayMs(long catchupRetryDelayMs) {
            c.catchupRetryDelayMs = catchupRetryDelayMs;
            return this;
        }

        public Builder streamStartTimeoutMs(long streamStartTimeoutMs) {
            c.streamStartTimeoutMs = streamStartTimeoutMs;
            return this;
        }

        public Builder streamReadTimeoutMs(long streamReadTimeoutMs) {
            c.streamReadTimeoutMs = streamReadTimeoutMs;
            return this;
        }

        public Builder groupFilters(Collection<Pattern> groupFilters) {
            c.groupFilters = new ArrayList<>(groupFilters);
            return this;
        }
    }
}
