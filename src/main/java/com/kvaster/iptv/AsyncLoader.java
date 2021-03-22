package com.kvaster.iptv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncLoader<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncLoader.class);

    public static AsyncLoader<String> stringLoader(long timeoutSec, long totalTimeoutSec, long retryDelayMs, ScheduledExecutorService scheduler) {
        return new AsyncLoader<>(timeoutSec, totalTimeoutSec, retryDelayMs, scheduler, HttpResponse.BodyHandlers::ofString);
    }

    public static AsyncLoader<byte[]> bytesLoader(long timeoutSec, long totalTimeoutSec, long retryDelayMs, ScheduledExecutorService scheduler) {
        return new AsyncLoader<>(timeoutSec, totalTimeoutSec, retryDelayMs, scheduler, HttpResponse.BodyHandlers::ofByteArray);
    }

    private final long timeoutSec;
    private final long totalTimeoutSec;
    private final long retryDelayMs;
    private final ScheduledExecutorService scheduler;
    private final Supplier<HttpResponse.BodyHandler<T>> handlerSupplier;

    public AsyncLoader(
            long timeoutSec, long totalTimeoutSec, long retryDelayMs, ScheduledExecutorService scheduler,
            Supplier<HttpResponse.BodyHandler<T>> handlerSupplier
    ) {
        this.timeoutSec = timeoutSec;
        this.totalTimeoutSec = totalTimeoutSec;
        this.retryDelayMs = retryDelayMs;
        this.scheduler = scheduler;
        this.handlerSupplier = handlerSupplier;
    }

    public CompletableFuture<T> loadAsync(String msg, String url, HttpClient httpClient) {
        final String rid = RequestCounter.next();

        var future = new CompletableFuture<T>();
        loadAsync(msg, url, 0, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(totalTimeoutSec), rid, future, httpClient);
        return future;
    }

    private void loadAsync(
            String msg,
            String url,
            int retryNo,
            long expireTime,
            String rid,
            CompletableFuture<T> future,
            HttpClient httpClient
    ) {
        LOG.info("{}loading {}, retry: {}, url: {}", rid, msg, retryNo, url);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        httpClient.sendAsync(req, handlerSupplier.get())
                .orTimeout(timeoutSec, TimeUnit.SECONDS)
                .whenComplete((resp, err) -> {
                    if (HttpUtils.isOk(resp, err, rid)) {
                        future.complete(resp.body());
                    } else {
                        if (System.currentTimeMillis() < expireTime) {
                            LOG.warn("{}will retry", rid);

                            scheduler.schedule(
                                    () -> loadAsync(msg, url, retryNo + 1, expireTime, rid, future, httpClient),
                                    retryDelayMs,
                                    TimeUnit.MILLISECONDS
                            );
                        } else {
                            LOG.error("{}failed", rid);
                            future.complete(null);
                        }
                    }
                });
    }
}
