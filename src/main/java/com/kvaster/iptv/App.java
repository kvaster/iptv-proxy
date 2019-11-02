package com.kvaster.iptv;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            LOG.info("Loading config...");

            File configFile = new File(System.getProperty("config", "config.yml"));

            ProxyConfig config = ConfigLoader.loadConfig(configFile, ProxyConfig.class);

            ProxyService service = new ProxyService(config);

            Runtime.getRuntime().addShutdownHook(new Thread(service::stopService));
            service.startService();
        } catch (Exception e) {
            LOG.error("error", e);
            System.exit(1);
        }
    }
}
