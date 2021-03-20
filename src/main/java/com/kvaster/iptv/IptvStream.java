package com.kvaster.iptv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvStream implements Subscriber<List<ByteBuffer>> {
    private static final Logger LOG = LoggerFactory.getLogger(IptvStream.class);

    private final HttpServerExchange exchange;

    private final Queue<ByteBuffer> buffers = new LinkedBlockingQueue<>();
    private final AtomicBoolean busy = new AtomicBoolean();

    private volatile Subscription subscription;

    private final static ByteBuffer END_MARKER = ByteBuffer.allocate(0);
    private final static List<ByteBuffer> END_ARRAY_MARKER = Collections.singletonList(END_MARKER);

    private final String rid;

    private final SpeedMeter readMeter;
    private final SpeedMeter writeMeter;

    private final IptvUser user;
    private final long userTimeout;

    private final ScheduledExecutorService scheduler;
    private final long readTimeout;

    private volatile long timeoutTime;
    private volatile ScheduledFuture<?> timeoutFuture;

    public IptvStream(HttpServerExchange exchange, String rid, IptvUser user, long userTimeout, long readTimeout, ScheduledExecutorService scheduler) {
        this.exchange = exchange;
        this.rid = rid;

        this.user = user;
        this.userTimeout = userTimeout;

        this.scheduler = scheduler;
        this.readTimeout = readTimeout;

        readMeter = new SpeedMeter(rid + "read: ");
        writeMeter = new SpeedMeter(rid + "write: ");

        updateReadTimeout();
        timeoutFuture = scheduler.schedule(this::onTimeout, readTimeout, TimeUnit.MILLISECONDS);
    }

    private void updateReadTimeout() {
        timeoutTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + readTimeout;
    }

    private void onTimeout() {
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        if (now >= timeoutTime) {
            LOG.warn("{}read timeout on loading stream", rid);
            finish();
        } else {
            timeoutFuture = scheduler.schedule(this::onTimeout, timeoutTime - now, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (this.subscription != null) {
            LOG.error("{}already subscribed", rid);
            subscription.cancel();
            return;
        }

        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    private void finish() {
        // cancel any timeouts
        timeoutFuture.cancel(false);

        // subscription can't be null at this place
        subscription.cancel();

        onNext(END_ARRAY_MARKER);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        int len = 0;
        for (ByteBuffer b : item) {
            len += b.remaining();
        }
        readMeter.processed(len);

        buffers.addAll(item);

        if (busy.compareAndSet(false, true)) {
            sendNext();
        }

        subscription.request(Long.MAX_VALUE);
    }

    private void sendNext() {
        ByteBuffer b;
        while ((b = buffers.poll()) != null) {
            if (!sendNext(b)) {
                return;
            }
        }

        busy.set(false);
    }

    private boolean sendNext(ByteBuffer b) {
        user.lock();
        try {
            user.setExpireTime(System.currentTimeMillis() + userTimeout);
        } finally {
            user.unlock();
        }

        updateReadTimeout();

        if (b == END_MARKER) {
            exchange.endExchange();
            //LOG.debug("{}write complete", rid);
            writeMeter.finish();

            return true;
        }

        AtomicBoolean completed = new AtomicBoolean(false);

        final int len = b.remaining();
        exchange.getResponseSender().send(b, new IoCallback() {
            @Override
            public void onComplete(HttpServerExchange exchange, Sender sender) {
                writeMeter.processed(len);

                if (!completed.compareAndSet(false, true)) {
                    sendNext();
                }
            }

            @Override
            public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
                LOG.warn("{}error on sending stream: {}", rid, exception.getMessage());
                finish();
            }
        });

        return !completed.compareAndSet(false, true);
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.warn("{}error on loading stream: {}", rid, throwable.getMessage());
        finish();
    }

    @Override
    public void onComplete() {
        //LOG.debug("{}read complete", rid);
        readMeter.finish();
        finish();
    }
}
