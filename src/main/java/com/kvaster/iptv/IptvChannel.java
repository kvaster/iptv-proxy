package com.kvaster.iptv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvChannel {
    private static final Logger LOG = LoggerFactory.getLogger(IptvChannel.class);

    private final String id;
    private final String name;
    private final String[] info;

    private final Random rand = new Random();

    private final List<IptvServerChannel> serverChannels = new ArrayList<>();

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

    public IptvServerChannel acquire(String userId) {
        List<IptvServerChannel> scs = new ArrayList<>(serverChannels);
        Collections.shuffle(scs, rand);

        for (IptvServerChannel sc : scs) {
            if (sc.acquire(userId)) {
                return sc;
            }
        }

        LOG.info("[{}] can't acquire channel: {}", userId, name);

        return null;
    }
}
