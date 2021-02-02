package com.kvaster.iptv.m3u;

import java.util.Map;
import java.util.Set;

public class M3uChannel {
    private final String url;

    private final String name;

    private final Set<String> groups;

    private final Map<String, String> props;

    public M3uChannel(String url, String name, Set<String> groups, Map<String, String> props) {
        this.url = url;
        this.name = name;
        this.groups = groups;
        this.props = props;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public String getProp(String key) {
        return props.get(key);
    }

    public Map<String, String> getProps() {
        return props;
    }
}
