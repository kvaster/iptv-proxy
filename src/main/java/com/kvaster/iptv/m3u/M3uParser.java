package com.kvaster.iptv.m3u;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M3uParser {
    private static final Logger LOG = LoggerFactory.getLogger(M3uParser.class);

    private static final Pattern TAG_PAT = Pattern.compile("#(\\w+)(?:[ :](.*))?");
    private static final Pattern PROP_PAT = Pattern.compile(" *([\\w-_]+)=\"([^\"]*)\"(.*)");
    private static final Pattern PROP_NONSTD_PAT = Pattern.compile(" *([\\w-_]+)=([^\"][^ ]*)(.*)");
    private static final Pattern INFO_PAT = Pattern.compile("([-+0-9]+) ?(.*)");

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
                            String prop = parseProps(m.group(2), m3uProps = new HashMap<>()).strip();
                            if (!prop.isEmpty()) {
                                LOG.warn("malformed property: {}", prop);
                            }
                        }
                        break;

                    case "EXTINF":
                        String infoLine = m.group(2);
                        m = INFO_PAT.matcher(infoLine);
                        if (m.matches()) {
                            name = parseProps(m.group(2), props = new HashMap<>()).strip();
                            if (name.startsWith(",")) {
                                name = name.substring(1).strip();
                            }
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

    private static String parseProps(String line, Map<String, String> props) {
        String postfix = "";
        List<String> malformedProps = new ArrayList<>();

        while (line.length() > 0) {
            Matcher m = PROP_PAT.matcher(line);
            if (!m.matches()) {
                m = PROP_NONSTD_PAT.matcher(line);
            }
            if (m.matches()) {
                props.put(m.group(1), m.group(2));
                line = m.group(3).strip();
                postfix = line;

                if (!malformedProps.isEmpty()) {
                    malformedProps.forEach(prop -> LOG.warn("malformed property: {}", prop));
                    malformedProps.clear();
                }
            } else {
                // try to continue parsing properties
                int idx = line.indexOf(' ');
                if (idx < 0) {
                    idx = line.length();
                }

                malformedProps.add(line.substring(0, idx));

                line = line.substring(idx).strip();
            }
        }

        return postfix;
    }
}
