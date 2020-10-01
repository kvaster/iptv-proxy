package com.kvaster.iptv;

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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final long CHANNELS_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60);
    private static final long CHANNELS_CONNECT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private static final long CHANNELS_RETRY_DELAY_MS = 1000;

    private static final Pattern PAT_TVGID = Pattern.compile(".*tvg-id=\"(?<tvgid>[^\"]+)\".*");

    private final Undertow undertow;
    private final Timer timer = new Timer();

    private final BaseUrl baseUrl;
    private final String tokenSalt;

    private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

    private final List<IptvServer> servers;
    private volatile Map<String, IptvChannel> channels = new HashMap<>();
    private Map<String, IptvServerChannel> serverChannelsByUrl = new HashMap<>();

    private final Map<String, IptvUser> users = new ConcurrentHashMap<>();

    private final boolean allowAnonymous;
    private final Set<String> allowedUsers;

    private final int connectTimeoutSec;

    public IptvProxyService(IptvProxyConfig config) {
        baseUrl = new BaseUrl(config.getBaseUrl(), config.getForwardedPass());

        this.tokenSalt = config.getTokenSalt();

        this.allowAnonymous = config.getAllowAnonymous();
        this.allowedUsers = config.getUsers();

        this.connectTimeoutSec = config.getConnectTimeoutSec();

        undertow = Undertow.builder()
                .addHttpListener(config.getPort(), config.getHost())
                .setHandler(this)
                .build();

        List<IptvServer> ss = new ArrayList<>();
        config.getServers().forEach((sc) -> ss.add(new IptvServer(sc)));
        servers = Collections.unmodifiableList(ss);
    }

    public void startService() {
        LOG.info("starting");

        updateChannels();

        undertow.start();

        LOG.info("started");
    }

    public void stopService() {
        LOG.info("stopping");

        timer.cancel();
        undertow.stop();

        LOG.info("stopped");
    }

    private void scheduleChannelsUpdate(long delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Thread(() -> updateChannels()).start();
            }
        }, delay);
    }

    private void updateChannels() {
        if (updateChannelsImpl()) {
            scheduleChannelsUpdate(TimeUnit.MINUTES.toMillis(240));
        } else {
            scheduleChannelsUpdate(TimeUnit.MINUTES.toMillis(1));
        }
    }

    private boolean updateChannelsImpl() {
        LOG.info("updating channels");

        Map<String, IptvChannel> chs = new HashMap<>();
        Map<String, IptvServerChannel> byUrl = new HashMap<>();

        Digest digest = Digest.sha256();

        Map<IptvServer, CompletableFuture<String>> loads = new HashMap<>();
        servers.forEach((s) -> loads.put(s, loadChannelsAsync(s.getName(), s.getUrl(), s.getHttpClient())));

        for (IptvServer server : servers) {
            String channels = null;

            try {
                channels = loads.get(server).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("error waiting for channels load", e);
            }

            if (channels == null) {
                return false;
            }

            LOG.info("parsing playlist: {}", server.getName());

            String name = null;
            String chanId = null;
            List<String> info = new ArrayList<>(10); // magic number, usually we have only 1-3 lines of info tags

            for (String line : channels.split("\n")) {
                line = line.trim();

                //noinspection StatementWithEmptyBody
                if (line.startsWith("#EXTM3U")) {
                    // do nothing - m3u format tag
                } else if (line.startsWith("#")) {
                    info.add(line);

                    if (line.startsWith("#EXTINF")) {
                        int idx = line.lastIndexOf(',');
                        if (idx >= 0) {
                            name = line.substring(idx + 1);
                        }

                        Matcher m = PAT_TVGID.matcher(line);
                        if (m.matches()) {
                            chanId = m.group("tvgid");
                        }
                    }
                } else {
                    if (name == null) {
                        LOG.warn("skipping malformed channel: {}, server: {}", line, server.getName());
                    } else {
                        // TODO we need proper ID generation in order to be able to merge channels
                        String id = digest.digest(chanId == null ? name : chanId);
                        //String id = digest.digest(line);
                        final String _name = name;
                        IptvChannel channel = chs.computeIfAbsent(id, k -> new IptvChannel(id, _name, info.toArray(new String[0])));

                        IptvServerChannel serverChannel = serverChannelsByUrl.get(line);
                        if (serverChannel == null) {
                            serverChannel = new IptvServerChannel(server, line, baseUrl.forPath('/' + id), id, name, connectTimeoutSec, timer);
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

        LOG.info("channels updated.");

        return true;
    }

    private CompletableFuture<String> loadChannelsAsync(String name, String url, HttpClient httpClient) {
        final String rid = RequestCounter.next();

        var future = new CompletableFuture<String>();
        loadChannelsAsync(name, url, httpClient, 0, System.currentTimeMillis() + CHANNELS_TIMEOUT_MS, rid, future);
        return future;
    }

    private void loadChannelsAsync(
            String name, String url, HttpClient httpClient, int retryNo,
            long expireTime, String rid, CompletableFuture<String> future
    ) {
        LOG.info("{}loading playlist: {}, retry: {}", rid, name, retryNo);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(CHANNELS_CONNECT_TIMEOUT_MS))
                .build();

        httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, err) -> {
                    if (HttpUtils.isOk(resp, err, rid)) {
                        future.complete(resp.body());
                    } else {
                        if (System.currentTimeMillis() < expireTime) {
                            LOG.warn("{}will retry", rid);

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    loadChannelsAsync(name, url, httpClient, retryNo + 1, expireTime, rid, future);
                                }
                            }, CHANNELS_RETRY_DELAY_MS);
                        } else {
                            LOG.error("{}failed", rid);
                            future.complete(null);
                        }
                    }
                });
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

        if (path.startsWith("m3u")) {
            return handleM3u(exchange, path);
        }

        // channels
        int idx = path.indexOf('/');
        if (idx < 0) {
            LOG.warn("wrong request: {}", exchange.getRequestPath());
            return false;
        }

        String ch = path.substring(0, idx);
        path = path.substring(idx + 1);

        IptvChannel channel = channels.get(ch);
        if (channel == null) {
            LOG.warn("channel not found: {}, for request: {}", ch, exchange.getRequestPath());
            return false;
        }

        // we need user if this is not m3u request
        String token = exchange.getQueryParameters().getOrDefault("t", new ArrayDeque<>()).peek();
        String user = getUserFromToken(token);

        // pass user name from another iptv-proxy
        String proxyUser = exchange.getRequestHeaders().getFirst(IptvServer.PROXY_USER_HEADER);

        // no token, or user is not verified
        if (user == null) {
            LOG.warn("invalid user token: {}, proxyUser: {}", token, proxyUser);
            return false;
        }

        if (proxyUser != null) {
            user = user + ':' + proxyUser;
        }

        IptvUser iu = users.computeIfAbsent(user, (u) -> new IptvUser(u, timer, users::remove));
        iu.lock();
        try {
            IptvServerChannel serverChannel = iu.getServerChannel(channel);
            if (serverChannel == null) {
                return false;
            }

            return serverChannel.handle(exchange, path, iu, token);
        } finally {
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

    private String generateUser() {
        return String.valueOf(idCounter.incrementAndGet());
    }

    private String generateToken(String user) {
        return user + '-' + Digest.md5(user + tokenSalt);
    }

    private boolean handleM3u(HttpServerExchange exchange, String path) {
        String user = null;

        int idx = path.indexOf('/');
        if (idx >= 0) {
            user = path.substring(idx + 1);
            if (!allowedUsers.contains(user)) {
                user = null;
            }
        }

        if (user == null && allowAnonymous) {
            user = generateUser();
        }

        if (user == null) {
            LOG.warn("user not defined for request: {}", exchange.getRequestPath());
            return false;
        }

        String token = generateToken(user);

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
            sb.append(baseUrl.getBaseUrl(exchange)).append('/').append(ch.getId()).append("/channel.m3u8?t=").append(token).append("\n");
        });

        exchange.getResponseSender().send(sb.toString());

        return true;
    }
}
