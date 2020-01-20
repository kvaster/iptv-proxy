package com.kvaster.iptv;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

/**
 * Created by kva on 14:02 20.01.2020
 */
public class RequestCounter {
    private static final boolean isDebug = LoggerFactory.getLogger(RequestCounter.class).isDebugEnabled();

    private static final AtomicInteger counter = new AtomicInteger();

    public static String next() {
        if (isDebug) {
            int c = counter.incrementAndGet() % 100000;
            return String.format("%05d| ", c);
        } else {
            return "";
        }
    }
}
