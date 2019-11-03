package com.kvaster.iptv;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

    private final IptvServer server;
    private final String channelUrl;
    private final String baseUrl;
    private final String channelId;
    private final String channelName;

    private final HttpClient httpClient;

    private volatile Map<String, String> streamMap = new HashMap<>();

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

    public IptvServerChannel(IptvServer server, String channelUrl, String baseUrl, String channelId, String channelName, HttpClient httpClient) {
        this.server = server;
        this.channelUrl = channelUrl;
        this.baseUrl = baseUrl;
        this.channelId = channelId;
        this.channelName = channelName;

        this.httpClient = httpClient;
    }

    public String getChannelId() {
        return channelId;
    }

    public boolean acquire() {
        if (server.acquire()) {
            LOG.info("Channel acquired: {} / {}", channelName, server.getName());
            return true;
        }

        return false;
    }

    public void release() {
        LOG.info("Channel release: {} / {}", channelName, server.getName());
        server.release();
    }

    public boolean handle(HttpServerExchange exchange, String path, String user, String token) {
        if ("channel.m3u8".equals(path)) {
            handleInfo(exchange, user, token);
            return true;
        } else {
            String streamUrl = streamMap.get(path);
            LOG.info("Stream: {}, url: {}", user, streamUrl);

            // TODO allow redirects through caching nginx or direct redirects
//            if (true) {
//                exchange.setStatusCode(StatusCodes.FOUND);
//                exchange.getResponseHeaders().add(Headers.LOCATION, streamUrl);
//                exchange.endExchange();
//                return true;
//            }

            if (streamUrl != null) {
                exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(streamUrl))
                            .timeout(Duration.ofSeconds(5))
                            .build();

                    httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofPublisher())
                            .thenAccept((resp) -> {
                                if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
                                    exchange.setStatusCode(resp.statusCode());
                                    exchange.getResponseSender().send("error");
                                } else {
                                    for (HttpString header : HEADERS) {
                                        resp.headers().firstValue(header.toString()).ifPresent(value -> exchange.getResponseHeaders().add(header, value));
                                    }

                                    resp.body().subscribe(new IptvStream(exchange));
                                }
                            });
                });

                return true;
            }
        }

        return false;
    }

    private void handleInfo(HttpServerExchange exchange, String user, String token) {
        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
            LOG.info("Channel: {}, user: {}, url: {}", channelName, user, channelUrl);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(channelUrl))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
                            exchange.setStatusCode(resp.statusCode());
                            exchange.getResponseSender().send("error");
                        } else {
                            String[] info = resp.body().split("\n");

                            Digest digest = Digest.sha256();
                            StringBuilder sb = new StringBuilder();

                            Map<String, String> streamMap = new HashMap<>();

                            for (String l : info) {
                                l = l.trim();

                                if (l.startsWith("#")) {
                                    sb.append(l).append("\n");
                                } else {
                                    String path = digest.digest(l) + ".ts";
                                    streamMap.put(path, l);
                                    sb.append(baseUrl).append('/').append(path).append("?t=").append(token).append("\n");
                                }
                            }

                            this.streamMap = streamMap;
                            exchange.setStatusCode(HttpURLConnection.HTTP_OK);
                            exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/vnd.apple.mpegurl");
                            exchange.getResponseSender().send(sb.toString());
                            exchange.endExchange();
                        }
                    });
        });
    }
}
