package com.kvaster.iptv;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.kvaster.utils.digest.Digest;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.SameThreadExecutor;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvServerChannel {
    private static final Logger LOG = LoggerFactory.getLogger(IptvServerChannel.class);

    private static final String TAG_EXTINF = "#EXTINF:";
    private static final String TAG_PROGRAM_DATE_TIME = "#EXT-X-PROGRAM-DATE-TIME:";

    private final IptvServer server;
    private final String channelUrl;
    private final BaseUrl baseUrl;
    private final String channelId;
    private final String channelName;

    private final HttpClient httpClient;
    private final int timeoutSec;

    private volatile long failedUntil;

    private static class Stream {
        String url;
        long startTime;
        long durationMillis;

        Stream(String url, long startTime, long durationMillis) {
            this.url = url;
            this.startTime = startTime;
            this.durationMillis = durationMillis;
        }

        @Override
        public String toString() {
            return "[url: " + url + ", start: " + new Date(startTime) + ", duration: " + (durationMillis / 1000) + "s]";
        }
    }

    private volatile Map<String, Stream> streamMap = new HashMap<>();

    private static final HttpString[] HEADERS = {
        Headers.CONTENT_TYPE,
        Headers.CONTENT_LENGTH,
        Headers.CONNECTION,
        Headers.DATE,
        new HttpString("access-control-allow-origin="),
        new HttpString("access-control-allow-headers"),
        new HttpString("access-control-allow-methods"),
        new HttpString("access-control-expose-headers"),
        new HttpString("x-memory"),
        new HttpString("x-route-time"),
        new HttpString("x-run-time")
    };

    public IptvServerChannel(
            IptvServer server, String channelUrl, BaseUrl baseUrl, String channelId, String channelName,
            HttpClient httpClient, int timeoutSec
    ) {
        this.server = server;
        this.channelUrl = channelUrl;
        this.baseUrl = baseUrl;
        this.channelId = channelId;
        this.channelName = channelName;

        this.httpClient = httpClient;
        this.timeoutSec = timeoutSec;
    }

    public String getChannelId() {
        return channelId;
    }

    public boolean acquire(String userId) {
        if (System.currentTimeMillis() < failedUntil) {
            return false;
        }

        if (server.acquire()) {
            LOG.info("[{}] channel acquired: {} / {}", userId, channelName, server.getName());
            return true;
        }

        return false;
    }

    public void release(String userId) {
        LOG.info("[{}] channel released: {} / {}", userId, channelName, server.getName());
        server.release();
    }

    private HttpRequest.Builder createRequest(String url, IptvUser user) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSec));

        // send user id to next iptv-proxy
        if (server.getSendUser()) {
            builder.header(IptvServer.PROXY_USER_HEADER, user.getId());
        }

        return builder;
    }

    public boolean handle(HttpServerExchange exchange, String path, IptvUser user, String token) {
        if ("channel.m3u8".equals(path)) {
            handleInfo(exchange, user, token);
            return true;
        } else {
            Stream stream = streamMap.get(path);

            if (stream != null) {
                final String rid = RequestCounter.next();
                LOG.info("{}[{}] stream: {}", rid, user.getId(), stream);

                // usually we expect that player will try not to decrease buffer size
                // so we may expect that player will try to buffer more segments with durationMillis delay
                user.setExpireTime(System.currentTimeMillis() + stream.durationMillis * 2 + 1000);

                if (!server.getProxyStream()) {
                    LOG.info("{}redirecting stream to direct url", rid);
                    exchange.setStatusCode(StatusCodes.FOUND);
                    exchange.getResponseHeaders().add(Headers.LOCATION, stream.url);
                    exchange.endExchange();
                    return true;
                }

                exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
                    HttpRequest req = createRequest(stream.url, user).build();

                    httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofPublisher())
                            .whenComplete((resp, err) -> {
                                if (HttpUtils.isOk(resp, err, exchange, rid)) {
                                    for (HttpString header : HEADERS) {
                                        resp.headers().firstValue(header.toString()).ifPresent(value -> exchange.getResponseHeaders().add(header, value));
                                    }

                                    resp.body().subscribe(new IptvStream(exchange, rid));
                                }
                            });
                });

                return true;
            }
        }

        return false;
    }

    private void handleInfo(HttpServerExchange exchange, IptvUser user, String token) {
        user.setExpireTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec + 1));

        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
            String rid = RequestCounter.next();
            LOG.info("{}[{}] channel: {}, url: {}", rid, user.getId(), channelName, channelUrl);

            HttpRequest req = createRequest(channelUrl, user).build();

            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((resp, err) -> {
                        if (HttpUtils.isOk(resp, err, exchange, rid)) {
                            String[] info = resp.body().split("\n");

                            Digest digest = Digest.sha256();
                            StringBuilder sb = new StringBuilder();

                            Map<String, Stream> streamMap = new HashMap<>();

                            long m3uStart = 0;

                            long durationMillis = 0;
                            long startTime = 0;

                            for (String l : info) {
                                l = l.trim();

                                if (l.startsWith("#")) {
                                    if (l.startsWith(TAG_EXTINF)) {
                                        String v = l.substring(TAG_EXTINF.length());
                                        int idx = v.indexOf(',');
                                        if (idx >= 0) {
                                            v = v.substring(0, idx);
                                        }

                                        try {
                                            durationMillis = new BigDecimal(v).multiply(new BigDecimal(1000)).longValue();
                                        } catch (NumberFormatException e) {
                                            // do nothing
                                        }
                                    } else if (l.startsWith(TAG_PROGRAM_DATE_TIME)) {
                                        try {
                                            ZonedDateTime dateTime = ZonedDateTime.parse(l.substring(TAG_PROGRAM_DATE_TIME.length()), DateTimeFormatter.ISO_DATE_TIME);
                                            m3uStart = startTime = dateTime.toInstant().toEpochMilli();
                                        } catch (Exception e) {
                                            // do nothing
                                        }
                                    }

                                    sb.append(l).append("\n");
                                } else {
                                    String path = digest.digest(l) + ".ts";
                                    streamMap.put(path, new Stream(l, startTime, durationMillis));
                                    sb.append(baseUrl.getBaseUrl(exchange)).append('/').append(path).append("?t=").append(token).append("\n");

                                    startTime = durationMillis == 0 ? 0 : startTime + durationMillis;
                                    durationMillis = 0;
                                }
                            }

                            this.streamMap = streamMap;
                            exchange.setStatusCode(HttpURLConnection.HTTP_OK);
                            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/vnd.apple.mpegurl");
                            exchange.getResponseSender().send(sb.toString());
                            exchange.endExchange();

                            LOG.info("{}m3u start: {}, end: {}", rid, new Date(m3uStart), new Date(startTime));
                        } else {
                            if (server.getChannelFailedMs() > 0) {
                                user.lock();
                                try {
                                    LOG.warn("{}channel failed", rid);
                                    failedUntil = System.currentTimeMillis() + server.getChannelFailedMs();
                                    user.onRemove();
                                } finally {
                                    user.unlock();
                                }
                            }
                        }
                    });
        });
    }
}
