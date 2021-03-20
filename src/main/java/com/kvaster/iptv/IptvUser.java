package com.kvaster.iptv;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvUser {
    private static final Logger LOG = LoggerFactory.getLogger(IptvUser.class);

    private final String id;
    private final Lock lock = new ReentrantLock();

    private final ScheduledExecutorService scheduler;
    private final BiConsumer<String, IptvUser> unregister;

    private long expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);

    private long timeoutTime;
    private ScheduledFuture<?> timeoutFuture;

    private volatile IptvServerChannel serverChannel;

    public IptvUser(String id, ScheduledExecutorService scheduler, BiConsumer<String, IptvUser> unregister) {
        this.id = id;
        this.scheduler = scheduler;
        this.unregister = unregister;

        LOG.info("[{}] user created", id);
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        // small optimization to have less removes and Future recreates in priority queue
        if (timeoutTime == 0 || timeoutTime > expireTime) {
            schedule();
        }

        lock.unlock();
    }

    private void schedule() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
        }

        // 100ms jitter
        timeoutFuture = scheduler.schedule(this::removeIfNeed, expireDelay() + 100, TimeUnit.MILLISECONDS);
        timeoutTime = expireTime;
    }

    private void removeIfNeed() {
        lock();
        try {
            if (System.currentTimeMillis() < expireTime) {
                timeoutTime = 0;
                schedule();
            } else {
                unregister.accept(id, this);
                releaseChannel();

                LOG.info("[{}] user removed", id);
            }
        } finally {
            unlock();
        }
    }

    public String getId() {
        return id;
    }

    private long expireDelay() {
        return Math.max(100, expireTime - System.currentTimeMillis());
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = Math.max(this.expireTime, expireTime);
    }

    public void releaseChannel() {
        if (serverChannel != null) {
            serverChannel.release(id);
            serverChannel = null;
        }
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
