package com.kvaster.iptv.m3u;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestM3u {
    private static final Logger LOG = LoggerFactory.getLogger(TestM3u.class);

    public static void main(String[] args) {
        try {
            String path = "../ilook.m3u8";

            String content = Files.readString(Path.of(path));

            M3uDoc doc = M3uParser.parse(content);
        } catch (Exception e) {
            LOG.error("error", e);
        }
    }
}
