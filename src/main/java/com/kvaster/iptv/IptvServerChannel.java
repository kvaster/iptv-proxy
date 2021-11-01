package com.kvaster.iptv;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
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
    private static final String TAG_TARGET_DURATION = "#EXT-X-TARGETDURATION:";

    private final IptvServer server;
    private final String channelUrl;
    private final BaseUrl baseUrl;
    private final String channelId;
    private final String channelName;

    private final HttpClient httpClient;

    private final ScheduledExecutorService scheduler;

    private volatile long failedUntil;

    private final long defaultInfoTimeout;
    private final long defaultCatchupTimeout;

    private final boolean isHls;

    private static class Stream {
        String path;
        String url;
        String header;
        long durationMillis;

        Stream(String path, String url, String header, long durationMillis) {
            this.path = path;
            this.url = url;
            this.header = header;
            this.durationMillis = durationMillis;
        }

        @Override
        public String toString() {
            return "[path: " + path + ", url: " + url + ", duration: " + (durationMillis / 1000f) + "s]";
        }
    }

    private static class Streams {
        List<Stream> streams = new ArrayList<>();
        // cache only for 100ms to avoid burst requests
        long expireTimeNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);
        long maxDuration = 0;
    }

    private interface StreamsConsumer {
        void onInfo(Streams streams, int statusCode);
    }

    private static class UserStreams {
        Streams streams;
        List<StreamsConsumer> consumers = new ArrayList<>();
        Map<String, Stream> streamMap = new HashMap<>();
        long infoTimeout;
        String channelUrl;
        boolean isCatchup;

        UserStreams(long infoTimeout, String channelUrl) {
            this.infoTimeout = infoTimeout;
            this.channelUrl = channelUrl;
        }

        List<StreamsConsumer> getAndClearConsumers() {
            var c = consumers;
            consumers = new ArrayList<>();
            return c;
        }
    }

    private final Map<String, UserStreams> userStreams = new ConcurrentHashMap<>();

    private static final Set<String> HEADERS = new HashSet<>(Arrays.asList(
        "content-type",
        "content-length",
        "connection",
        "date",
        //"access-control-allow-origin",
        "access-control-allow-headers",
        "access-control-allow-methods",
        "access-control-expose-headers",
        "x-memory",
        "x-route-time",
        "x-run-time"
    ));

    public IptvServerChannel(
            IptvServer server, String channelUrl, BaseUrl baseUrl,
            String channelId, String channelName, ScheduledExecutorService scheduler
    ) {
        this.server = server;
        this.channelUrl = channelUrl;
        this.baseUrl = baseUrl;
        this.channelId = channelId;
        this.channelName = channelName;

        this.httpClient = server.getHttpClient();

        this.scheduler = scheduler;

        defaultInfoTimeout = TimeUnit.SECONDS.toMillis(Math.max(server.getInfoTotalTimeoutSec(), server.getInfoTimeoutSec()) + 1);
        defaultCatchupTimeout = TimeUnit.SECONDS.toMillis(Math.max(server.getCatchupTotalTimeoutSec(), server.getCatchupTimeoutSec()) + 1);

        try {
            URI uri = new URI(channelUrl);
            isHls = uri.getPath().endsWith(".m3u8") || uri.getPath().endsWith(".m3u");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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

        userStreams.remove(userId);
    }

    private HttpRequest createRequest(String url, IptvUser user) {
        HttpRequest.Builder builder = server.createRequest(url);

        // send user id to next iptv-proxy
        if (user != null && server.getSendUser()) {
            builder.header(IptvServer.PROXY_USER_HEADER, user.getId());
        }

        return builder.build();
    }

    private long calculateTimeout(long duration) {
        // usually we expect that player will try not to decrease buffer size
        // so we may expect that player will try to buffer more segments with durationMillis delay
        // kodi is downloading two or three buffers at same time
        // use 10 seconds for segment duration if unknown (5 or 7 seconds are usual values)
        return (duration == 0 ? TimeUnit.SECONDS.toMillis(10) : duration) * 3 + TimeUnit.SECONDS.toMillis(1);
    }

    public boolean handle(HttpServerExchange exchange, String path, IptvUser user, String token) {
        if ("channel.m3u8".equals(path)) {
            if (!isHls) {
                String url = exchange.getRequestURL().replace("channel.m3u8", "");
                String q = exchange.getQueryString();
                if (q != null && !q.isBlank()) {
                    url += '?' + q;
                }

                exchange.setStatusCode(StatusCodes.FOUND);
                exchange.getResponseHeaders().add(Headers.LOCATION, url);
                exchange.endExchange();
                return true;
            }

            handleInfo(exchange, user, token);
            return true;
        } else if ("".equals(path)) {
            final String rid = RequestCounter.next();
            LOG.info("{}[{}] stream: {}", rid, user.getId(), channelUrl);

            runStream(rid, exchange, user, channelUrl, TimeUnit.SECONDS.toMillis(1));

            return true;
        } else {
            // iptv user is synchronized (locked) at this place
            UserStreams us = userStreams.get(user.getId());
            if (us == null) {
                LOG.warn("[{}] no streams set up: {}", user.getId(), exchange.getRequestPath());
                return false;
            }

            Stream stream = us.streamMap.get(path);

            if (stream == null) {
                LOG.warn("[{}] stream not found: {}", user.getId(), exchange.getRequestPath());
                return false;
            } else {
                final String rid = RequestCounter.next();
                LOG.info("{}[{}] stream: {}", rid, user.getId(), stream);

                long timeout = calculateTimeout(us.streams.maxDuration);
                user.setExpireTime(System.currentTimeMillis() + timeout);

                runStream(rid, exchange, user, stream.url, timeout);

                return true;
            }
        }
    }

    private void runStream(String rid, HttpServerExchange exchange, IptvUser user, String url, long timeout) {
        if (!server.getProxyStream()) {
            LOG.info("{}redirecting stream to direct url", rid);
            exchange.setStatusCode(StatusCodes.FOUND);
            exchange.getResponseHeaders().add(Headers.LOCATION, url);
            exchange.endExchange();
            return;
        }

        // be sure we have time to start stream
        user.setExpireTime(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(server.getStreamStartTimeoutSec()) + 100);

        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
            long startNanos = System.nanoTime();

            // configure buffering according to undertow buffers settings for best performance
            httpClient.sendAsync(createRequest(url, user), HttpResponse.BodyHandlers.ofPublisher())
                    .orTimeout(server.getStreamStartTimeoutSec(), TimeUnit.SECONDS)
                    .whenComplete((resp, err) -> {
                        if (HttpUtils.isOk(resp, err, exchange, rid, startNanos)) {
                            resp.headers().map().forEach((name, values) -> {
                                if (HEADERS.contains(name.toLowerCase())) {
                                    exchange.getResponseHeaders().addAll(new HttpString(name), values);
                                }
                            });

                            exchange.getResponseHeaders().add(HttpUtils.ACCESS_CONTROL, "*");

                            long readTimeoutMs = TimeUnit.SECONDS.toMillis(server.getStreamReadTimeoutSec());
                            resp.body().subscribe(new IptvStream(exchange, rid, user, Math.max(timeout, readTimeoutMs), readTimeoutMs, scheduler, startNanos));
                        }
                    });
        });
    }

    private void handleInfo(HttpServerExchange exchange, IptvUser user, String token) {
        UserStreams us = createUserStreams(exchange, user);

        // we'll wait maximum one second for stream download start after loading info
        user.setExpireTime(System.currentTimeMillis() + us.infoTimeout);

        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
            String rid = RequestCounter.next();
            LOG.info("{}[{}] channel: {}, url: {}", rid, user.getId(), channelName, us.channelUrl);
            long startNanos = System.nanoTime();
            loadCachedInfo((streams, statusCode) -> {
                if (streams == null) {
                    LOG.warn("{}[{}] error loading streams info: {}", rid, user.getId(), statusCode);

                    exchange.setStatusCode(statusCode);
                    exchange.getResponseSender().send("error");
                } else {
                    LOG.info("{}[{}] success: {}ms", rid, user.getId(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));

                    StringBuilder sb = new StringBuilder();

                    streams.streams.forEach(s -> sb
                            .append(s.header)
                            .append(baseUrl.getBaseUrl(exchange))
                            .append('/').append(s.path).append("?t=").append(token).append("\n")
                    );

                    exchange.setStatusCode(HttpURLConnection.HTTP_OK);
                    exchange.getResponseHeaders()
                            .add(Headers.CONTENT_TYPE, "application/x-mpegUrl")
                            .add(HttpUtils.ACCESS_CONTROL, "*");
                    exchange.getResponseSender().send(sb.toString());
                    exchange.endExchange();
                }
            }, user, us);
        });
    }

    private void loadCachedInfo(StreamsConsumer consumer, IptvUser user, UserStreams us) {
        Streams s = null;
        boolean startReq = false;

        user.lock();
        try {
            if (us.streams != null && (us.streams.expireTimeNanos - System.nanoTime()) > 0) {
                s = us.streams;
            } else {
                us.streams = null;
                startReq = us.consumers.size() == 0;
                us.consumers.add(consumer);
            }
        } finally {
            user.unlock();
        }

        if (startReq) {
            loadInfo(
                    RequestCounter.next(),
                    0,
                    System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(us.isCatchup ? server.getCatchupTotalTimeoutSec() : server.getInfoTotalTimeoutSec()),
                    user,
                    us
            );
        } else if (s != null) {
            consumer.onInfo(s, 0);
        }
    }

    private void loadInfo(String rid, int retryNo, long expireTime, IptvUser user, UserStreams us) {
        LOG.info("{}[{}] loading channel: {}, url: {}, retry: {}", rid, user.getId(), channelName, us.channelUrl, retryNo);

        final long startNanos = System.nanoTime();
        httpClient.sendAsync(createRequest(us.channelUrl, user), HttpResponse.BodyHandlers.ofString())
                .orTimeout(us.isCatchup ? server.getCatchupTimeoutSec() : server.getInfoTimeoutSec(), TimeUnit.SECONDS)
                .whenComplete((resp, err) -> {
                    if (HttpUtils.isOk(resp, err, rid, startNanos)) {
                        String[] info = resp.body().split("\n");

                        Digest digest = Digest.sha256();
                        StringBuilder sb = new StringBuilder();

                        Map<String, Stream> streamMap = new HashMap<>();
                        Streams streams = new Streams();

                        long durationMillis = 0;

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
                                        streams.maxDuration = Math.max(streams.maxDuration, durationMillis);
                                    } catch (NumberFormatException e) {
                                        // do nothing
                                    }
                                } else if (l.startsWith(TAG_TARGET_DURATION)) {
                                    try {
                                        long targetDuration = new BigDecimal(l.substring(TAG_TARGET_DURATION.length())).multiply(new BigDecimal(1000)).longValue();
                                        streams.maxDuration = Math.max(streams.maxDuration, targetDuration);
                                    } catch (NumberFormatException e) {
                                        // do nothing
                                    }
                                }

                                sb.append(l).append("\n");
                            } else {
                                // transform url
                                if (!l.startsWith("http://") && !l.startsWith("https://")) {
                                    int idx = channelUrl.lastIndexOf('/');
                                    if (idx >= 0) {
                                        l = channelUrl.substring(0, idx + 1) + l;
                                    }
                                }

                                try {
                                    URI streamUri = new URI(l);
                                    // we need to redownload m3u8 if m3u8 is found insteadof .ts streams
                                    if (streamUri.getPath().endsWith(".m3u8") || streamUri.getPath().endsWith(".m3u")) {
                                        URI baseUri = new URI(us.channelUrl);
                                        us.channelUrl = baseUri.resolve(streamUri).toString();
                                        loadInfo(rid, retryNo, expireTime, user, us);
                                        return;
                                    }
                                } catch (URISyntaxException e) {
                                    // probably we need to just skip this ?
                                    LOG.trace("error parsing stream url", e);
                                }


                                String path = digest.digest(l) + ".ts";
                                Stream s = new Stream(path, l, sb.toString(), durationMillis);
                                streamMap.put(path, s);
                                streams.streams.add(s);

                                sb = new StringBuilder();

                                durationMillis = 0;
                            }
                        }

                        List<StreamsConsumer> cs;

                        user.lock();
                        try {
                            us.streamMap = streamMap;
                            us.streams = streams;

                            us.infoTimeout = calculateTimeout(streams.maxDuration);

                            user.setExpireTime(System.currentTimeMillis() + us.infoTimeout);

                            cs = us.getAndClearConsumers();
                        } finally {
                            user.unlock();
                        }

                        LOG.info("{}[{}] m3u maxDuration: {}s", rid, user.getId(), streams.maxDuration / 1000f);

                        cs.forEach(c -> c.onInfo(streams, -1));
                    } else {
                        if (System.currentTimeMillis() < expireTime) {
                            LOG.info("{}[{}] will retry", rid, user.getId());

                            scheduler.schedule(
                                    () -> loadInfo(rid, retryNo + 1, expireTime, user, us),
                                    us.isCatchup ? server.getCatchupRetryDelayMs() : server.getInfoRetryDelayMs(),
                                    TimeUnit.MILLISECONDS
                            );
                        } else {
                            if (server.getChannelFailedMs() > 0) {
                                user.lock();
                                try {
                                    LOG.warn("{}[{}] channel failed", rid, user.getId());
                                    failedUntil = System.currentTimeMillis() + server.getChannelFailedMs();
                                    user.releaseChannel();
                                } finally {
                                    user.unlock();
                                }
                            } else {
                                LOG.warn("{}[{}] streams failed", rid, user.getId());
                            }

                            int statusCode = resp == null ? HttpURLConnection.HTTP_INTERNAL_ERROR : resp.statusCode();
                            us.getAndClearConsumers().forEach(c -> c.onInfo(null, statusCode));
                        }
                    }
                });
    }

    private UserStreams createUserStreams(HttpServerExchange exchange, IptvUser user) {
        String url = createChannelUrl(exchange);

        // user is locked here
        UserStreams us = userStreams.get(user.getId());
        if (us == null || !us.channelUrl.equals(url)) {
            boolean isCatchup = exchange.getQueryParameters().containsKey("utc") ||
                    exchange.getQueryParameters().containsKey("lutc");
            us = new UserStreams(isCatchup ? defaultCatchupTimeout : defaultInfoTimeout, url);
            us.isCatchup = isCatchup;
            userStreams.put(user.getId(), us);
        }

        return us;
    }

    private String createChannelUrl(HttpServerExchange exchange) {
        Map<String, String> qp = new TreeMap<>();
        exchange.getQueryParameters().forEach((k, v) -> {
            // skip our token tag
            if (!"t".equals(k)) {
                if (v.size() > 0) {
                    qp.put(k, v.getFirst());
                }
            }
        });

        if (qp.isEmpty()) {
            return channelUrl;
        }

        URI uri;

        try {
            uri = new URI(channelUrl);
        } catch (URISyntaxException se) {
            throw new RuntimeException(se);
        }

        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            for (String pair : uri.getRawQuery().split("&")) {
                int idx = pair.indexOf('=');
                String key = URLDecoder.decode(idx >= 0 ? pair.substring(0, idx) : pair, StandardCharsets.UTF_8);
                String value = idx < 0 ? null : URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                qp.putIfAbsent(key, value);
            }
        }

        StringBuilder q = new StringBuilder();
        qp.forEach((k, v) -> {
            if (q.length() > 0) {
                q.append('&');
            }

            q.append(URLEncoder.encode(k, StandardCharsets.UTF_8));
            if (v != null) {
                q.append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        });

        try {
            return new URI(
                    uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                    uri.getPort(), uri.getPath(), q.toString(), uri.getFragment()
            ).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
