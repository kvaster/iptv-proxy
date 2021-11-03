package com.kvaster.iptv;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kva on 21:39 27.03.2020
 */
public class SpeedMeter {
    private static final Logger LOG = LoggerFactory.getLogger(SpeedMeter.class);

    private static final int KB = 1024;
    private static final int MB = 1024 * 1024;

    private final String rid;

    private final long time;
    private long bytes;

    private long partTime;
    private long partBytes;

    private long reqStartNanos;

    private static long getMonotonicMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    public SpeedMeter(String rid, long reqStartNanos) {
        this.rid = rid;
        this.time = this.partTime = getMonotonicMillis();
        this.reqStartNanos = reqStartNanos;
    }

    public void processed(long len) {
        if (bytes == 0) {
            LOG.debug("{}start", rid);
        }

        bytes += len;
        partBytes += len;

        long now = getMonotonicMillis();
        if ((now - partTime) > 1000) {
            logPart();
        }
    }

    public void finish() {
        long now = getMonotonicMillis();
        if ((now - partTime) > 1000) {
            logPart();
        }

        LOG.debug("{}finished: {}, speed: {}/s, {}ms",
                rid, format(bytes),
                format(bytes * 1000 / (now - time)),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reqStartNanos));
    }

    private void logPart() {
        long now = getMonotonicMillis();
        long delta = Math.max(1, now - partTime);

        LOG.debug("{}progress: {} speed: {}/s", rid, format(partBytes), format(partBytes * 1000 / delta));
        partTime = now;
        partBytes = 0;
    }

    private String format(long value) {
        if (value < KB) {
            return String.format("%db", value);
        } else if (value < MB) {
            return String.format("%.2fKb", (double)value / KB);
        } else {
            return String.format("%.2fMb", (double)value / MB);
        }
    }
}
