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
import io.undertow.util.SameThreadExecutor;
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
            LOG.info("Channel aqcuired: {} / {}", channelName, server.getName());
            return true;
        }

        return false;
    }

    public void release() {
        LOG.info("Channel release: {} / {}", channelName, server.getName());
        server.release();
    }

    public boolean handle(HttpServerExchange exchange, String path, String token) {
        if ("channel.m3u8".equals(path)) {
            handleInfo(exchange, token);
            return true;
        } else {
            String streamUrl = streamMap.get(path);

            // LOG.info("download: {}", streamUrl);

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
                                    String contentType = resp.headers().firstValue(Headers.CONTENT_TYPE_STRING).orElse(null);
                                    String contentLength = resp.headers().firstValue(Headers.CONTENT_LENGTH_STRING).orElse(null);

                                    if (contentType != null) {
                                        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, contentType);
                                    }

                                    if (contentLength != null) {
                                        exchange.getResponseHeaders().add(Headers.CONTENT_LENGTH, contentLength);
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

    private void handleInfo(HttpServerExchange exchange, String token) {
        exchange.dispatch(SameThreadExecutor.INSTANCE, () -> {
            LOG.info("channel: {}", channelUrl);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(channelUrl))
                    .timeout(Duration.ofSeconds(2))
                    .build();

            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
                            exchange.setStatusCode(resp.statusCode());
                            exchange.getResponseSender().send("error");
                        } else {
                            String[] info = resp.body().split("\n");

                            Digest digest = Digest.sha512();
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
