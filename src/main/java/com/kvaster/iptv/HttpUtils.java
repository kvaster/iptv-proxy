package com.kvaster.iptv;

import java.net.HttpURLConnection;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
            LOG.warn(rid + "io error: {}", err == null ? "timeout" : err.getMessage());
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

    public static void addBase64Authorization(HttpRequest.Builder reqBuilder, String user, String password) {
        String auth = user + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);
        reqBuilder.header("Authorization", authHeader);
    }
}
