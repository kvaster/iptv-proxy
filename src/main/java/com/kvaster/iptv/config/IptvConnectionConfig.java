package com.kvaster.iptv.config;

public class IptvConnectionConfig {
    private String url;
    private int maxConnections;
    private String login;
    private String password;
    private String userAgent;

    protected IptvConnectionConfig() {
    }

    public String getUrl() {
        return url;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public static class Builder {
        private final IptvConnectionConfig c = new IptvConnectionConfig();

        public IptvConnectionConfig build() {
            return c;
        }

        public Builder url(String url) {
            c.url = url;
            return this;
        }

        public Builder maxConnections(int maxConnections) {
            c.maxConnections = maxConnections;
            return this;
        }

        public Builder login(String login) {
            c.login = login;
            return this;
        }

        public Builder password(String password) {
            c.password = password;
            return this;
        }

        public Builder userAgent(String userAgent) {
            c.userAgent = userAgent;
            return this;
        }
    }
}
