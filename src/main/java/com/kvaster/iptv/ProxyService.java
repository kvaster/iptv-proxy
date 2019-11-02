package com.kvaster.iptv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyService implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyService.class);

    private final HttpClient httpClient;
    private final Undertow undertow;

    private final String playlistUrl;
    private final String baseUrl;

    private final Map<String, IptvChannel> channels = new HashMap<>();

    private String playlist;

    /*
    Для m3u:
    Content-Disposition: attachment; filename=playlist.m3u
    Content-Type: audio/mpegurl

    Для m3u8:
    Content-Type: application/vnd.apple.mpegurl
     */

    public ProxyService(ProxyConfig config) {
        this(config.getHost(), config.getPort(), config.getPlaylistUrl(), config.getBaseUrl());
    }

    public ProxyService(String host, int port, String playlistUrl, String baseUrl) {
        this.playlistUrl = playlistUrl;
        this.baseUrl = baseUrl;

        httpClient = HttpClient.newBuilder().build();

        undertow = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(this)
                .build();
    }

    public void startService() throws IOException {
        LOG.info("Starting...");

        loadPlaylist();
        undertow.start();

        LOG.info("Started.");
    }

    public void stopService() {
        LOG.info("Stopping...");

        undertow.stop();

        LOG.info("Stopped.");
    }

    private void loadPlaylist() throws IOException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(playlistUrl))
                .timeout(Duration.ofSeconds(5))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException();
            } else {
                String[] lines = resp.body().split("\n");

                StringBuilder sb = new StringBuilder();
                int channel = 1;

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("#")) {
                        sb.append(line).append("\n");
                    } else {
                        String chanId = String.valueOf(channel++);
                        String chanBaseUrl = baseUrl + '/' + chanId;
                        channels.put(chanId, new IptvChannel(line, chanBaseUrl, httpClient));
                        sb.append(chanBaseUrl).append("/channel.m3u8\n");
                    }
                }

                playlist = sb.toString();
            }
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        }
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (!handleInternal(exchange)) {
            exchange.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
            exchange.getResponseSender().send("N/A");
        }
    }

    private boolean handleInternal(HttpServerExchange exchange) throws Exception {
        String path = exchange.getRequestPath();

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if ("m3u".equals(path)) {
            exchange.getResponseHeaders()
                    .add(Headers.CONTENT_TYPE, "audio/mpegurl")
                    .add(Headers.CONTENT_DISPOSITION, "attachment; filename=playlist.m3u");
            exchange.getResponseSender().send(playlist);

            return true;
        }

        // channels
        int idx = path.indexOf('/');
        if (idx < 0) {
            return false;
        }

        String ch = path.substring(0, idx);
        path = path.substring(idx + 1);

        IptvChannel channel = channels.get(ch);
        if (channel == null) {
            return false;
        }

        return channel.handle(exchange, path);
    }
}
