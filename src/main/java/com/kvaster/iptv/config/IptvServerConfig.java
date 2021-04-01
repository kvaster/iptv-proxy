package com.kvaster.iptv.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IptvServerConfig {
    private String name;
    private List<IptvConnectionConfig> connections;
    private String xmltvUrl;
    private boolean sendUser;
    private boolean proxyStream = true;
    private long channelFailedMs;
    private long infoTimeoutSec = 2;
    private long infoTotalTimeoutSec = 5;
    private long infoRetryDelayMs = 100;
    private long catchupTimeoutSec = 5;
    private long catchupTotalTimeoutSec = 10;
    private long catchupRetryDelayMs = 100;
    private long streamStartTimeoutSec = 2;
    private long streamReadTimeoutSec = 2;

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

    public long getStreamStartTimeoutSec() {
        return streamStartTimeoutSec;
    }

    public long getStreamReadTimeoutSec() {
        return streamReadTimeoutSec;
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

        public Builder infoTimeoutSec(long infoTimeoutSec) {
            c.infoTimeoutSec = infoTimeoutSec;
            return this;
        }

        public Builder infoTotalTimeoutSec(long infoTotalTimeoutSec) {
            c.infoTotalTimeoutSec = infoTotalTimeoutSec;
            return this;
        }

        public Builder infoRetryDelayMs(long infoRetryDelayMs) {
            c.infoRetryDelayMs = infoRetryDelayMs;
            return this;
        }

        public Builder catchupTimeoutSec(long catchupTimeoutSec) {
            c.catchupTimeoutSec = catchupTimeoutSec;
            return this;
        }

        public Builder catchupTotalTimeoutSec(long catchupTotalTimeoutSec) {
            c.catchupTotalTimeoutSec = catchupTotalTimeoutSec;
            return this;
        }

        public Builder catchupRetryDelayMs(long catchupRetryDelayMs) {
            c.catchupRetryDelayMs = catchupRetryDelayMs;
            return this;
        }

        public Builder streamStartTimeoutSec(long streamStartTimeoutSec) {
            c.streamStartTimeoutSec = streamStartTimeoutSec;
            return this;
        }

        public Builder streamReadTimeoutSec(long streamReadTimeoutSec) {
            c.streamReadTimeoutSec = streamReadTimeoutSec;
            return this;
        }
    }
}
