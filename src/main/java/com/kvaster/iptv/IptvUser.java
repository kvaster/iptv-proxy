package com.kvaster.iptv;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvUser {
    private static final Logger LOG = LoggerFactory.getLogger(IptvUser.class);

    private final String id;

    private long lastAccess = System.currentTimeMillis();
    private TimerTask task;

    private volatile IptvServerChannel serverChannel;

    public IptvUser(String id) {
        this.id = id;

        LOG.info("User created: {}", id);
    }

    public String getId() {
        return id;
    }

    public boolean needRemove(long delay) {
        return (System.currentTimeMillis() - lastAccess) >= delay;
    }

    public void onAccess(TimerTask task) {
        lastAccess = System.currentTimeMillis();

        if (this.task != null) {
            this.task.cancel();
        }

        this.task = task;
    }

    public synchronized void onRemove() {
        if (serverChannel != null) {
            serverChannel.release();
            serverChannel = null;
        }

        LOG.info("User removed: {}", id);
    }

    public synchronized IptvServerChannel getServerChannel(IptvChannel channel) {
        if (serverChannel != null) {
            if (serverChannel.getChannelId().equals(channel.getId())) {
                return serverChannel;
            }

            serverChannel.release();
        }

        serverChannel = channel.acquire();

        return serverChannel;
    }
}
