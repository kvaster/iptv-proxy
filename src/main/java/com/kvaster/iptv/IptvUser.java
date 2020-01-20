package com.kvaster.iptv;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvUser {
    private static final Logger LOG = LoggerFactory.getLogger(IptvUser.class);

    private final String id;
    private final Lock lock = new ReentrantLock();

    private long expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);

    private TimerTask task;

    private volatile IptvServerChannel serverChannel;

    public IptvUser(String id) {
        this.id = id;

        LOG.info("[{}] user created", id);
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public String getId() {
        return id;
    }

    public long expireDelay() {
        return expireTime - System.currentTimeMillis();
    }

    public boolean isExpired(long delay) {
        return expireTime <= 0;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public void addExpireTime(long durationMillis) {
        expireTime += durationMillis;
    }

    public void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void setTask(TimerTask task) {
        cancelTask();
        this.task = task;
    }

    public void onRemove() {
        if (serverChannel != null) {
            serverChannel.release(id);
            serverChannel = null;
        }

        LOG.info("[{}] user removed", id);
    }

    public IptvServerChannel getServerChannel(IptvChannel channel) {
        if (serverChannel != null) {
            if (serverChannel.getChannelId().equals(channel.getId())) {
                return serverChannel;
            }

            serverChannel.release(id);
        }

        expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);

        serverChannel = channel.acquire(id);

        return serverChannel;
    }
}
