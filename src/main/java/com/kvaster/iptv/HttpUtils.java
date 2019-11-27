package com.kvaster.iptv;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    public static boolean isOk(HttpResponse<?> resp, Throwable err, HttpServerExchange exchange) {
        if (resp == null) {
            LOG.warn("IO error", err);
            exchange.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            exchange.getResponseSender().send("error");
            return false;
        } else if (resp.statusCode() != HttpURLConnection.HTTP_OK) {
            LOG.warn("Bad status code: {}", resp.statusCode());
            exchange.setStatusCode(resp.statusCode());
            exchange.getResponseSender().send("error");
            return false;
        }

        return true;
    }
}
