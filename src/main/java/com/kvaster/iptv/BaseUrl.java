package com.kvaster.iptv;

import io.undertow.server.HttpServerExchange;

public class BaseUrl {
    private final String baseUrl;
    private final String forwardedPass;
    private final String path;

    public BaseUrl(String baseUrl, String forwardedPass) {
        this(baseUrl, forwardedPass, "");
    }

    private BaseUrl(String baseUrl, String forwardedPass, String path) {
        this.baseUrl = baseUrl;
        this.forwardedPass = forwardedPass;
        this.path = path;
    }

    public BaseUrl forPath(String path) {
        return new BaseUrl(baseUrl, forwardedPass, this.path + path);
    }

    public String getBaseUrl(HttpServerExchange exchange) {
        return getBaseUrlWithoutPath(exchange) + path;
    }

    private String getBaseUrlWithoutPath(HttpServerExchange exchange) {
        if (forwardedPass != null) {
            String fwd = exchange.getRequestHeaders().getFirst("Forwarded");
            if (fwd != null) {
                String pass = null;
                String baseUrl = null;
                for (String pair : fwd.split(";")) {
                    int idx = pair.indexOf('=');
                    if (idx >= 0) {
                        String key = pair.substring(0, idx);
                        String value = pair.substring(idx + 1);
                        if ("pass".equals(key)) {
                            pass = value;
                        } else if ("baseUrl".equals(key)) {
                            baseUrl = value;
                        }
                    }
                }

                if (baseUrl != null && forwardedPass.equals(pass)) {
                    return baseUrl;
                }
            }
        }

        if (baseUrl != null) {
            return baseUrl;
        }

        return exchange.getRequestScheme() + "://" + exchange.getHostAndPort();
    }
}
