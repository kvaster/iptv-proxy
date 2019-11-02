package com.kvaster.iptv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvStream implements Subscriber<List<ByteBuffer>> {
    private static final Logger LOG = LoggerFactory.getLogger(IptvStream.class);

    private final HttpServerExchange exchange;

    private Queue<ByteBuffer> buffers = new LinkedBlockingQueue<>();
    private AtomicBoolean busy = new AtomicBoolean();

    private Subscription subscription;

    public IptvStream(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public synchronized void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    private synchronized void cancel() {
        if (subscription != null) {
            subscription.cancel();
        }
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        buffers.addAll(item);

        if (busy.compareAndSet(false, true)) {
            sendNext();
        }

        subscription.request(1);
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
        AtomicBoolean completed = new AtomicBoolean(false);

        exchange.getResponseSender().send(b, new IoCallback() {
            @Override
            public void onComplete(HttpServerExchange exchange, Sender sender) {
                if (!completed.compareAndSet(false, true)) {
                    sendNext();
                }
            }

            @Override
            public void onException(HttpServerExchange exchange, Sender sender, IOException exception) {
                exchange.endExchange();
                cancel();
            }
        });

        return !completed.compareAndSet(false, true);
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.info("onError", throwable);
        exchange.endExchange();
    }

    @Override
    public void onComplete() {
        LOG.info("onComplete");
        exchange.endExchange();
    }
}
