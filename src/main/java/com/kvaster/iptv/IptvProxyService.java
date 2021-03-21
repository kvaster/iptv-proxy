package com.kvaster.iptv;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.kvaster.iptv.config.IptvProxyConfig;
import com.kvaster.iptv.m3u.M3uDoc;
import com.kvaster.iptv.m3u.M3uParser;
import com.kvaster.iptv.xmltv.XmltvChannel;
import com.kvaster.iptv.xmltv.XmltvDoc;
import com.kvaster.iptv.xmltv.XmltvUtils;
import com.kvaster.utils.digest.Digest;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IptvProxyService implements HttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IptvProxyService.class);

    private static class IptvServerGroup {
        final String name;
        final List<IptvServer> servers = new ArrayList<>();

        final String xmltvUrl;

        byte[] xmltvCache;

        IptvServerGroup(String name, String xmltvUrl) {
            this.name = name;
            this.xmltvUrl = xmltvUrl;
        }
    }

    private static final String TOKEN_TAG = "t";

    private final Undertow undertow;

    // use two threads instead of one
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);

    private final BaseUrl baseUrl;
    private final String tokenSalt;

    private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

    private final List<IptvServerGroup> serverGroups = new ArrayList<>();
    private volatile Map<String, IptvChannel> channels = new HashMap<>();
    private Map<String, IptvServerChannel> serverChannelsByUrl = new HashMap<>();

    private final Map<String, IptvUser> users = new ConcurrentHashMap<>();

    private final boolean allowAnonymous;
    private final Set<String> allowedUsers;

    private final AsyncLoader<String> channelsLoader;
    private final AsyncLoader<byte[]> xmltvLoader;
    private volatile byte[] xmltvData = null;

    private final HttpClient defaultHttpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public IptvProxyService(IptvProxyConfig config) {
        baseUrl = new BaseUrl(config.getBaseUrl(), config.getForwardedPass());

        this.tokenSalt = config.getTokenSalt();

        this.allowAnonymous = config.getAllowAnonymous();
        this.allowedUsers = config.getUsers();

        channelsLoader = AsyncLoader.stringLoader(config.getChannelsTimeoutSec(), config.getChannelsTotalTimeoutSec(), config.getChannelsRetryDelayMs(), scheduler);
        xmltvLoader = AsyncLoader.bytesLoader(config.getXmltvTimeoutSec(), config.getXmltvTotalTimeoutSec(), config.getXmltvRetryDelayMs(), scheduler);

        undertow = Undertow.builder()
                .addHttpListener(config.getPort(), config.getHost())
                .setHandler(this)
                .build();

        config.getServers().forEach((sc) -> {
            IptvServerGroup sg = new IptvServerGroup(sc.getName(), sc.getXmltvUrl());
            serverGroups.add(sg);
            sc.getConnections().forEach((cc) -> sg.servers.add(new IptvServer(sc, cc)));
        });
    }

    public void startService() {
        LOG.info("starting");

        updateChannels();

        undertow.start();

        LOG.info("started");
    }

    public void stopService() {
        LOG.info("stopping");

        try {
            scheduler.shutdownNow();
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                LOG.warn("scheduler is still running...");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOG.error("interrupted while stopping scheduler");
        }

        undertow.stop();

        LOG.info("stopped");
    }

    private void scheduleChannelsUpdate(long delayMins) {
        scheduler.schedule(() -> new Thread(this::updateChannels).start(), delayMins, TimeUnit.MINUTES);
    }

    private void updateChannels() {
        if (updateChannelsImpl()) {
            scheduleChannelsUpdate(240);
        } else {
            scheduleChannelsUpdate(1);
        }
    }

    private boolean updateChannelsImpl() {
        LOG.info("updating channels");

        Map<String, IptvChannel> chs = new HashMap<>();
        Map<String, IptvServerChannel> byUrl = new HashMap<>();

        Digest digest = Digest.sha256();
        Digest md5 = Digest.md5();

        Map<IptvServer, CompletableFuture<String>> loads = new HashMap<>();
        Map<IptvServerGroup, CompletableFuture<byte[]>> xmltvLoads = new HashMap<>();
        serverGroups.forEach((sg) -> {
            if (sg.xmltvUrl != null) {
                xmltvLoads.put(sg, xmltvLoader.loadAsync("xmltv: " + sg.name, sg.xmltvUrl, defaultHttpClient));
            }
            sg.servers.forEach(s -> loads.put(s, channelsLoader.loadAsync("playlist: " + s.getName(), s.getUrl(), s.getHttpClient())));
        });

        XmltvDoc newXmltv = new XmltvDoc()
                .setChannels(new ArrayList<>())
                .setProgrammes(new ArrayList<>())
                .setGeneratorName("iptvproxy");

        for (IptvServerGroup sg : serverGroups) {
            XmltvDoc xmltv = null;
            if (sg.xmltvUrl != null) {
                LOG.info("waiting for xmltv data to be downloaded");

                byte[] data = null;

                try {
                    data = xmltvLoads.get(sg).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.warn("error loading xmltv data");
                }

                if (data != null) {
                    LOG.info("parsing xmltv data");

                    xmltv = XmltvUtils.parseXmltv(data);
                    if (xmltv != null) {
                        sg.xmltvCache = data;
                    }
                }

                if (xmltv == null && sg.xmltvCache != null) {
                    xmltv = XmltvUtils.parseXmltv(sg.xmltvCache);
                }
            }

            Map<String, XmltvChannel> xmltvById = new HashMap<>();
            Map<String, XmltvChannel> xmltvByName = new HashMap<>();

            if (xmltv != null) {
                xmltv.getChannels().forEach(ch -> {
                    xmltvById.put(ch.getId(), ch);
                    if (ch.getDisplayNames() != null) {
                        ch.getDisplayNames().forEach(n -> xmltvByName.put(n.getText(), ch));
                    }
                });
            }

            Map<String, String> xmltvIds = new HashMap<>();

            for (IptvServer server : sg.servers) {
                LOG.info("parsing playlist: {}, url: {}", sg.name, server.getUrl());

                String channels = null;

                try {
                    channels = loads.get(server).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("error waiting for channels load", e);
                }

                if (channels == null) {
                    return false;
                }

                M3uDoc m3u = M3uParser.parse(channels);
                if (m3u == null) {
                    LOG.error("error parsing m3u, update skipped");
                    return false;
                }

                m3u.getChannels().forEach((c) -> {
                    // Unique ID will be formed from server name and channel name.
                    // It seems that there will be no any other suitable way to identify channel.
                    final String id = digest.digest(sg.name + "||" + c.getName());
                    final String url = c.getUrl();

                    IptvChannel channel = chs.get(id);
                    if (channel == null) {
                        String tvgId = c.getProp("tvg-id");
                        String tvgName = c.getProp("tvg-name");

                        XmltvChannel xmltvCh = null;
                        if (tvgId != null) {
                            xmltvCh = xmltvById.get(tvgId);
                        }
                        if (xmltvCh == null && tvgName != null) {
                            xmltvCh = xmltvByName.get(tvgName);
                            if (xmltvCh == null) {
                                xmltvCh = xmltvByName.get(tvgName.replace(' ', '_'));
                            }
                        }
                        if (xmltvCh == null) {
                            xmltvCh = xmltvByName.get(c.getName());
                        }

                        String logo = c.getProp("tvg-logo");
                        if (logo == null && xmltvCh != null && xmltvCh.getIcon() != null && xmltvCh.getIcon().getSrc() != null) {
                            logo = xmltvCh.getIcon().getSrc();
                        }

                        int days = 0;
                        String daysStr = c.getProp("tvg-rec");
                        if (daysStr == null) {
                            daysStr = c.getProp("catchup-days");
                        }
                        if (daysStr != null) {
                            try {
                                days = Integer.parseInt(daysStr);
                            } catch (NumberFormatException e) {
                                LOG.warn("error parsing catchup days: {}, channel: {}", daysStr, c.getName());
                            }
                        }

                        String xmltvId = xmltvCh == null ? null : xmltvCh.getId();
                        if (xmltvId != null) {
                            String newId = md5.digest(sg.name + '-' + xmltvId);
                            if (xmltvIds.putIfAbsent(xmltvId, newId) == null) {
                                newXmltv.getChannels().add(new XmltvChannel().setId(newId));
                            }
                            xmltvId = newId;
                        }

                        channel = new IptvChannel(id, c.getName(), logo, c.getGroups(), xmltvId, days);
                        chs.put(id, channel);
                    }

                    IptvServerChannel serverChannel = serverChannelsByUrl.get(url);
                    if (serverChannel == null) {
                        serverChannel = new IptvServerChannel(server, url, baseUrl.forPath('/' + id), id, c.getName(), scheduler);
                    }

                    channel.addServerChannel(serverChannel);

                    chs.put(id, channel);
                    byUrl.put(url, serverChannel);
                });
            }

            if (xmltv != null) {
                xmltv.getProgrammes().forEach(p -> {
                    String newId = xmltvIds.get(p.getChannel());
                    if (newId != null) {
                        newXmltv.getProgrammes().add(p.copy().setChannel(newId));
                    }
                });
            }
        }

        xmltvData = XmltvUtils.writeXmltv(newXmltv);
        channels = chs;
        serverChannelsByUrl = byUrl;

        LOG.info("channels updated.");

        return true;
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

        if (path.startsWith("epg.xml.gz")) {
            return handleEpg(exchange);
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
        String token = exchange.getQueryParameters().getOrDefault(TOKEN_TAG, new ArrayDeque<>()).peek();
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

        IptvUser iu = users.computeIfAbsent(user, (u) -> new IptvUser(u, scheduler, users::remove));
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
                .add(Headers.CONTENT_DISPOSITION, "attachment; filename=playlist.m3u")
                .add(HttpUtils.ACCESS_CONTROL, "*");

        List<IptvChannel> chs = new ArrayList<>(channels.values());
        chs.sort(Comparator.comparing(IptvChannel::getName));

        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");

        chs.forEach(ch -> {
            sb.append("#EXTINF:0");

            if (ch.getXmltvId() != null) {
                sb.append(" tvg-id=\"").append(ch.getXmltvId()).append('"');
            }

            if (ch.getLogo() != null) {
                sb.append(" tvg-logo=\"").append(ch.getLogo()).append('"');
            }

            if (ch.getCatchupDays() != 0) {
                sb.append(" catchup=\"shift\" catchup-days=\"").append(ch.getCatchupDays()).append('"');
            }

            sb.append(',').append(ch.getName()).append("\n");

            if (ch.getGroups().size() > 0) {
                sb.append("#EXTGRP:").append(String.join(";", ch.getGroups())).append("\n");
            }

            sb.append(baseUrl.getBaseUrl(exchange))
                    .append('/')
                    .append(ch.getId())
                    .append("/channel.m3u8?")
                    .append(TOKEN_TAG)
                    .append("=")
                    .append(token)
                    .append("\n");
        });

        exchange.getResponseSender().send(sb.toString());

        return true;
    }

    private boolean handleEpg(HttpServerExchange exchange) {
        byte[] epg = xmltvData;
        if (epg == null) {
            return false;
        }

        exchange.getResponseHeaders()
                .add(Headers.CONTENT_TYPE, "application/octet-stream")
                .add(Headers.CONTENT_DISPOSITION, "attachment; filename=epg.xml.gz")
                .add(Headers.CONTENT_LENGTH, Integer.toString(epg.length));

        exchange.getResponseSender().send(ByteBuffer.wrap(epg));

        return true;
    }
}
