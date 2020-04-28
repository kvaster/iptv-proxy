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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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

    private final Timer timer;

    private volatile long failedUntil;

    private static class Stream {
        String path;
        String url;
        String header;
        long startTime;
        long durationMillis;

        Stream(String path, String url, String header, long startTime, long durationMillis) {
            this.path = path;
            this.url = url;
            this.header = header;
            this.startTime = startTime;
            this.durationMillis = durationMillis;
        }

        @Override
        public String toString() {
            return "[path: " + path + ", url: " + url + ", start: " + new Date(startTime) + ", duration: " + (durationMillis / 1000) + "s]";
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
            IptvServer server, String channelUrl, BaseUrl baseUrl,
            String channelId, String channelName, int timeoutSec,
            Timer timer
    ) {
        this.server = server;
        this.channelUrl = channelUrl;
        this.baseUrl = baseUrl;
        this.channelId = channelId;
        this.channelName = channelName;

        this.httpClient = server.getHttpClient();
        this.timeoutSec = timeoutSec;

        this.timer = timer;
    }

    @Override
    public String toString() {
        return "[name: " + channelName + ", server: " + server.getName() + "]";
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
                // kodi is downloading two buffers at same time
                long timeout = stream.durationMillis * 2 + TimeUnit.SECONDS.toMillis(1);
                user.setExpireTime(System.currentTimeMillis() + timeout);

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

                                    resp.body().subscribe(new IptvStream(exchange, rid, user, timeout));
                                }
                            });
                });

                return true;
            }
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////

    private static class Streams {
        List<Stream> streams = new ArrayList<>();
        long expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);
    }

    private interface StreamsConsumer {
        void onInfo(Streams streams, int statusCode);
    }

    private List<StreamsConsumer> streamsConsumers = new ArrayList<>();
    private Streams streams;

    private synchronized List<StreamsConsumer> getAndClearStreamsConsumers() {
        List<StreamsConsumer> cs = streamsConsumers;
        streamsConsumers = new ArrayList<>();
        return cs;
    }

    private void handleInfo(HttpServerExchange exchange, IptvUser user, String token) {
        // we'll wait maximum one second for stream download start after loading info
        user.setExpireTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1));

        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
            String rid = RequestCounter.next();
            LOG.info("{}[{}] channel: {}, url: {}", rid, user.getId(), channelName, channelUrl);
            loadCachedInfo((streams, statusCode) -> {
                if (streams == null) {
                    LOG.warn("{}[{}] error loading streams info: {}", rid, user.getId(), statusCode);

                    exchange.setStatusCode(statusCode);
                    exchange.getResponseSender().send("error");
                } else {
                    LOG.info("{}[{}] ok", rid, user.getId());

                    StringBuilder sb = new StringBuilder();

                    streams.streams.forEach(s -> sb
                            .append(s.header)
                            .append(baseUrl.getBaseUrl(exchange))
                            .append('/').append(s.path).append("?t=").append(token).append("\n")
                    );

                    exchange.setStatusCode(HttpURLConnection.HTTP_OK);
                    exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/vnd.apple.mpegurl");
                    exchange.getResponseSender().send(sb.toString());
                    exchange.endExchange();
                }
            }, user);
        });
    }

    // TODO we must get rid of user info here
    // what happens - we'll be loading stream info on behalf of specific user, but what we should really do
    // is to keep per-channel connection. Probably we should have some way to mark channel as free
    // without timeouts
    private void loadCachedInfo(StreamsConsumer consumer, IptvUser user) {
        Streams s = null;
        boolean startReq = false;

        synchronized (this) {
            if (streams != null && System.currentTimeMillis() < streams.expireTime) {
                s = streams;
            } else {
                streams = null;
                startReq = streamsConsumers.size() == 0;
                streamsConsumers.add(consumer);
            }
        }

        if (startReq) {
            loadInfo(
                    RequestCounter.next(),
                    0,
                    System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(server.getRetryTimeoutSec()),
                    user
            );
        } else if (s != null) {
            consumer.onInfo(s, 0);
        }
    }

    // TODO we must get rid of user info here
    private void loadInfo(String rid, int retryNo, long expireTime, IptvUser user) {
        HttpRequest req = createRequest(channelUrl, user).build();

        LOG.info("{}loading channel: {}, url: {}, retry: {}", rid, channelName, channelUrl, retryNo);

        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> {
                    if (HttpUtils.isOk(resp, err, rid)) {
                        String[] info = resp.body().split("\n");

                        Digest digest = Digest.sha256();
                        StringBuilder sb = new StringBuilder();

                        Map<String, Stream> streamMap = new HashMap<>();
                        Streams streams = new Streams();

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
                                Stream s = new Stream(path, l, sb.toString(), startTime, durationMillis);
                                streamMap.put(path, s);
                                streams.streams.add(s);

                                sb = new StringBuilder();

                                startTime = durationMillis == 0 ? 0 : startTime + durationMillis;
                                // cache until end of life of current segment
                                streams.expireTime = Math.max(streams.expireTime, startTime);
                                durationMillis = 0;
                            }
                        }

                        List<StreamsConsumer> cs;

                        synchronized (this) {
                            this.streamMap = streamMap;
                            this.streams = streams;

                            cs = getAndClearStreamsConsumers();
                        }

                        cs.forEach(c -> c.onInfo(streams, -1));

                        LOG.info("{}m3u start: {}, end: {}", rid, new Date(m3uStart), new Date(startTime));
                    } else {
                        if (System.currentTimeMillis() < expireTime) {
                            LOG.info("{}will retry", rid);

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    loadInfo(rid, retryNo + 1, expireTime, user);
                                }
                            }, server.getRetryDelayMs());
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
                            } else {
                                LOG.warn("{}streams failed", rid);
                            }

                            int statusCode = resp == null ? HttpURLConnection.HTTP_INTERNAL_ERROR : resp.statusCode();
                            getAndClearStreamsConsumers().forEach(c -> c.onInfo(null, statusCode));
                        }
                    }
                });
    }
}
