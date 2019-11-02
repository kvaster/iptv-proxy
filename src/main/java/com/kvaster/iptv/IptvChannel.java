package com.kvaster.iptv;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvChannel {
    private static final Logger LOG = LoggerFactory.getLogger(IptvChannel.class);

    private final String id;
    private final String name;
    private final String[] info;

    private List<IptvServerChannel> serverChannels = new ArrayList<>();

    public IptvChannel(String id, String name, String[] info) {
        this.id = id;
        this.name = name;
        this.info = info;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String[] getInfo() {
        return info;
    }

    public void addServerChannel(IptvServerChannel serverChannel) {
        serverChannels.add(serverChannel);
    }

    public IptvServerChannel acquire() {
        for (IptvServerChannel sc : serverChannels) {
            if (sc.acquire()) {
                return sc;
            }
        }

        LOG.info("Can't acquire channel: {}", name);

        return null;
    }
}
