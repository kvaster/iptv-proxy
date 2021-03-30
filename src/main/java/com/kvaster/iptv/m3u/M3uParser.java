package com.kvaster.iptv.m3u;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M3uParser {
    private static final Logger LOG = LoggerFactory.getLogger(M3uParser.class);

    private static final Pattern TAG_PAT = Pattern.compile("#(\\w+)(?:[ :](.*))?");
    private static final Pattern PROP_PAT = Pattern.compile(" *([\\w-_]+)=\"([^\"]*)\"(.*)");
    private static final Pattern INFO_PAT = Pattern.compile("([-+0-9]+) ?(.*),([^,]+)");

    public static M3uDoc parse(String content) {
        Map<String, String> m3uProps = Collections.emptyMap();//new HashMap<>();
        List<M3uChannel> channels = new ArrayList<>();

        Set<String> groups = new HashSet<>();
        Map<String, String> props = null;
        String name = null;

        for (String line : content.split("\n")) {
            line = line.strip();

            Matcher m;

            if ((m = TAG_PAT.matcher(line)).matches()) {
                switch (m.group(1)) {
                    case "EXTM3U":
                        String p = m.group(2);
                        if (p != null) {
                            m3uProps = parseProps(m.group(2));
                        }
                        break;

                    case "EXTINF":
                        String infoLine = m.group(2);
                        m = INFO_PAT.matcher(infoLine);
                        if (m.matches()) {
                            name = m.group(3);
                            props = parseProps(m.group(2));
                        } else {
                            LOG.error("malformed channel info: {}", infoLine);
                            return null;
                        }
                        break;

                    case "EXTGRP":
                        for (String group : m.group(2).strip().split(";")) {
                            groups.add(group.strip());
                        }
                        break;

                    default:
                        LOG.warn("unknown m3u tag: {}", m.group(1));
                }
            } else if (!line.isEmpty()) {
                if (name == null) {
                    LOG.warn("url found while no info defined: {}", line);
                } else {
                    String group = props.remove("group-title");
                    if (group != null) {
                        groups.add(group);
                    }

                    channels.add(new M3uChannel(line, name, groups, props));

                    name = null;
                    groups = new HashSet<>();
                    props = null;
                }
            }
        }

        return new M3uDoc(channels, m3uProps);
    }

    private static Map<String, String> parseProps(String line) {
        Map<String, String> props = new HashMap<>();

        while (line.length() > 0) {
            Matcher m = PROP_PAT.matcher(line);
            if (m.matches()) {
                props.put(m.group(1), m.group(2));
                line = m.group(3).strip();
            } else {
                if (line.contains(" ")) {
                    String mailformedPart = line.substring(0, line.indexOf(" "));
                    line = line.substring(line.indexOf(" ") + 1);
                    LOG.warn("malformed properties: {}", mailformedPart);
                } else {
                    LOG.warn("malformed properties: {}", line);
                    break;
                }
            }
        }

        return props;
    }
}
