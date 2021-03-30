package com.kvaster.iptv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvChannel {
    private static final Logger LOG = LoggerFactory.getLogger(IptvChannel.class);

    private final String id;
    private final String name;
    private final String label;

    private final String logo;
    private final Set<String> groups;
    private final String xmltvId;
    private final String catchup;
    private final int catchupDays;

    private final Random rand = new Random();

    private final List<IptvServerChannel> serverChannels = new ArrayList<>();

    public IptvChannel(String id, String name, String label, String logo, Collection<String> groups, String xmltvId, String catchup, int catchupDays) {
        this.id = id;
        this.name = name;
        this.label = label;

        this.logo = logo;
        this.groups = Collections.unmodifiableSet(new TreeSet<>(groups));
        this.xmltvId = xmltvId;
        this.catchup = catchup;
        this.catchupDays = catchupDays;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getLogo() {
        return logo;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public String getXmltvId() {
        return xmltvId;
    }

    public String getCatchup() {
        return catchup;
    }

    public int getCatchupDays() {
        return catchupDays;
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
