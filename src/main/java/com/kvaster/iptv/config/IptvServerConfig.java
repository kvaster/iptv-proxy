package com.kvaster.iptv.config;

import java.util.List;

public class IptvServerConfig {
    private String name;
    private List<IptvConnectionConfig> connections;
    private String xmltvUrl;
    private boolean sortChannels;
    private String channelPrefix;
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

    protected IptvServerConfig() {
        // for deserialization
    }

    public IptvServerConfig(
            String name,
            List<IptvConnectionConfig> connections,
            String xmltvUrl,
            boolean sortChannels,
            String channelPrefix,
            boolean sendUser,
            boolean proxyStream,
            long channelFailedMs,
            long infoTimeoutSec,
            long infoRetryDelayMs
    ) {
        this.name = name;
        this.connections = connections;
        this.xmltvUrl = xmltvUrl;
        this.sortChannels = sortChannels;
        this.channelPrefix = channelPrefix;
        this.sendUser = sendUser;
        this.proxyStream = proxyStream;
        this.channelFailedMs = channelFailedMs;
        this.infoTimeoutSec = infoTimeoutSec;
        this.infoRetryDelayMs = infoRetryDelayMs;
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

    public boolean getSortChannels() {
        return sortChannels;
    }

    public String getChannelPrefix() {
        return channelPrefix;
    }
}
