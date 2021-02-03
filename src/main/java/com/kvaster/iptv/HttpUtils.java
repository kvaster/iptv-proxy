package com.kvaster.iptv;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    public static boolean isOk(HttpResponse<?> resp, Throwable err, String rid) {
        return isOk(resp, err, null, rid);
    }

    public static boolean isOk(HttpResponse<?> resp, Throwable err, HttpServerExchange exchange, String rid) {
        if (resp == null) {
            LOG.warn(rid + "io error: {}", err == null ? null : err.getMessage());
            if (exchange != null) {
                exchange.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
                exchange.getResponseSender().send("error");
            }
            return false;
        } else if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
            LOG.warn(rid + "bad status code: {}", resp.statusCode());
            if (exchange != null) {
                exchange.setStatusCode(resp.statusCode());
                exchange.getResponseSender().send("error");
            }
            return false;
        } else {
            LOG.debug("{}ok", rid);
        }

        return true;
    }
}
