package com.kvaster.iptv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.kvaster.iptv.config.IptvProxyConfig;
import com.kvaster.utils.digest.Digest;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvProxyService implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IptvProxyService.class);

    private final HttpClient httpClient;
    private final Undertow undertow;
    private final Timer timer = new Timer();

    private final String baseUrl;
    private final String tokenSalt;

    private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

    private final List<IptvServer> servers;
    private volatile Map<String, IptvChannel> channels = new HashMap<>();
    private Map<String, IptvServerChannel> serverChannelsByUrl = new HashMap<>();

    private final Map<String, IptvUser> users = new ConcurrentHashMap<>();

    public IptvProxyService(IptvProxyConfig config) {
        this.baseUrl = config.getBaseUrl();
        this.tokenSalt = config.getTokenSalt();

        httpClient = HttpClient.newBuilder().build();

        undertow = Undertow.builder()
                .addHttpListener(config.getPort(), config.getHost())
                .setHandler(this)
                .build();

        List<IptvServer> ss = new ArrayList<>();
        config.getServers().forEach((sc) -> ss.add(new IptvServer(sc.getName(), sc.getUrl(), sc.getMaxConnections())));
        servers = Collections.unmodifiableList(ss);
    }

    public void startService() throws IOException {
        LOG.info("Starting...");

        undertow.start();
        scheduleChannelsUpdate(1);

        LOG.info("Started.");
    }

    public void stopService() {
        LOG.info("Stopping...");

        timer.cancel();
        undertow.stop();

        LOG.info("Stopped.");
    }

    private void scheduleChannelsUpdate(long delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateChannels();
            }
        }, delay);
    }

    private void updateChannels() {
        if (updateChannelsImpl())
            scheduleChannelsUpdate(TimeUnit.MINUTES.toMillis(240));
        else
            scheduleChannelsUpdate(TimeUnit.MINUTES.toMillis(10));
    }

    private boolean updateChannelsImpl() {
        LOG.info("Updating channels...");

        Map<String, IptvChannel> chs = new HashMap<>();
        Map<String, IptvServerChannel> byUrl = new HashMap<>();

        Digest digest = Digest.sha256();

        for (IptvServer server : servers) {
            LOG.info("Loading playlist for: {}", server.getName());

            String channels = loadChannels(server.getUrl());
            if (channels == null) {
                return false;
            }

            String name = null;
            List<String> info = new ArrayList<>(10); // magic number, usually we have only 1-3 lines of info tags

            for (String line: channels.split("\n")) {
                line = line.trim();

                if (line.startsWith("#EXTM3U")) {
                    // do nothing - m3u format tag
                } else if (line.startsWith("#")) {
                    info.add(line);
                    int idx = line.lastIndexOf(',');
                    if (idx >= 0) {
                        name = line.substring(idx + 1);
                    }
                } else {
                    if (name == null) {
                        LOG.warn("Skipping malformed channel: {}", line);
                    } else {
                        String id = digest.digest(name);
                        final String _name = name;
                        IptvChannel channel = chs.computeIfAbsent(id, k -> new IptvChannel(id, _name, info.toArray(new String[0])));

                        IptvServerChannel serverChannel = serverChannelsByUrl.get(line);
                        if (serverChannel == null) {
                            serverChannel = new IptvServerChannel(server, line, baseUrl + '/' + id, id, name, httpClient);
                        }

                        channel.addServerChannel(serverChannel);

                        chs.put(id, channel);
                        byUrl.put(line, serverChannel);
                    }

                    name = null;
                    info.clear();
                }
            }
        }

        channels = chs;
        serverChannelsByUrl = byUrl;

        LOG.info("Channels updated.");

        return true;
    }

    private String loadChannels(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
                LOG.error("Can't load playlist - status code is: {}", resp.statusCode());
            } else {
                return resp.body();
            }
        } catch (IOException ie) {
            LOG.error("Can't load playlist - io error: {}", ie.getMessage());
        } catch (InterruptedException ie) {
            LOG.error("Interrupted while loading playlist");
        }

        return null;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (!handleInternal(exchange)) {
            exchange.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
            exchange.getResponseSender().send("N/A");
        }
    }

    private boolean handleInternal(HttpServerExchange exchange) {
        String path = exchange.getRequestPath();

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if ("m3u".equals(path)) {
            handleM3u(exchange);
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

        // we need user if this is not m3u request
        String token = exchange.getQueryParameters().getOrDefault("t", new ArrayDeque<>()).peek();
        String user = getUserFromToken(token);

        // no token, or user is not verified
        if (user == null) {
            return false;
        }

        IptvUser iu = users.computeIfAbsent(user, IptvUser::new);
        iu.lock();
        try {
            // cancel any expiring tasks
            iu.cancelTask();

            IptvServerChannel serverChannel = iu.getServerChannel(channel);
            if (serverChannel == null) {
                return false;
            }

            return serverChannel.handle(exchange, path, iu, token);
        } finally {
            // launch expiring task
            TimerTask t = new TimerTask() {
                @Override
                public void run() {
                    iu.lock();
                    try {
                        users.remove(iu.getId(), iu);
                        iu.onRemove();
                    } finally {
                        iu.unlock();
                    }
                }
            };

            iu.setTask(t);
            timer.schedule(t, iu.expireDelay() + 100); // add 100ms for timer

            iu.unlock();
        }
    }

    private String getUserFromToken(String token) {
        if (token == null) {
            return null;
        }

        int idx = token.indexOf('-');
        if (idx < 0) {
            return null;
        }

        String digest = token.substring(idx + 1);
        String user = token.substring(0, idx);

        if (digest.equals(Digest.md5(user + tokenSalt))) {
            return user;
        }

        return null;
    }

    private String generateToken() {
        String user = String.valueOf(idCounter.incrementAndGet());
        return user + '-' + Digest.md5(user + tokenSalt);
    }

    private void handleM3u(HttpServerExchange exchange) {
        String token = generateToken();

        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, "audio/mpegurl")
                .add(Headers.CONTENT_DISPOSITION, "attachment; filename=playlist.m3u");

        List<IptvChannel> chs = new ArrayList<>(channels.values());
        chs.sort(Comparator.comparing(IptvChannel::getName));

        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");

        chs.forEach(ch -> {
            for (String i : ch.getInfo()) {
                sb.append(i).append("\n");
            }
            sb.append(baseUrl).append('/').append(ch.getId()).append("/channel.m3u8?t=").append(token).append("\n");
        });

        exchange.getResponseSender().send(sb.toString());
    }
}
