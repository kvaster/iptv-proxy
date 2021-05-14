package com.kvaster.iptv;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    public static HttpString ACCESS_CONTROL = new HttpString("Access-Control-Allow-Origin");

    public static boolean isOk(HttpResponse<?> resp, Throwable err, String rid) {
        return isOk(resp, err, null, rid);
    }

    public static boolean isOk(HttpResponse<?> resp, Throwable err, HttpServerExchange exchange, String rid) {
        if (resp == null) {
            String errMsg = (err == null || err instanceof TimeoutException) ? "timeout" : (err.getMessage() == null ? err.toString() : err.getMessage());
            LOG.warn(rid + "io error: {}", errMsg);
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
