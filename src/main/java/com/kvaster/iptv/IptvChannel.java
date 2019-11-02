package com.kvaster.iptv;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.SameThreadExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvChannel {
    private static final Logger LOG = LoggerFactory.getLogger(IptvChannel.class);

    private final String channelUrl;
    private final String baseUrl;

    private final HttpClient httpClient;

    private volatile Map<String, String> streamMap = new HashMap<>();

    public IptvChannel(String channelUrl, String baseUrl, HttpClient httpClient) {
        this.channelUrl = channelUrl;
        this.baseUrl = baseUrl;

        this.httpClient = httpClient;
    }

    public boolean handle(HttpServerExchange exchange, String path) {
        if ("channel.m3u8".equals(path)) {
            handleInfo(exchange);
            return true;
        } else {
            String streamUrl = streamMap.get(path);

            LOG.info("download: {}", streamUrl);

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

    private void handleInfo(HttpServerExchange exchange) {
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

                            Digester digester = new Digester();
                            StringBuilder sb = new StringBuilder();

                            Map<String, String> streamMap = new HashMap<>();

                            for (String l : info) {
                                l = l.trim();

                                if (l.startsWith("#")) {
                                    sb.append(l).append("\n");
                                } else {
                                    String path = digester.sha512(l) + ".ts";
                                    streamMap.put(path, l);
                                    sb.append(baseUrl).append('/').append(path).append("\n");
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

    private static class Digester {
        static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        final MessageDigest md;

        Digester() {
            try {
                md = MessageDigest.getInstance("SHA-512");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String sha512(String str) {
            md.update(str.getBytes());
            byte[] hash = md.digest();
            md.reset();

            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(HEX[(b & 0xf0) >> 8]).append(HEX[b & 0x0f]);
            }

            return sb.toString();
        }
    }
}
