package com.kvaster.iptv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            LOG.info("Starting...");
        } catch (Exception e) {
            LOG.error("error", e);
        }
    }
}
