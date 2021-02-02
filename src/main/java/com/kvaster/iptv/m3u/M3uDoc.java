package com.kvaster.iptv.m3u;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class M3uDoc {
    private final List<M3uChannel> channels;

    private final Map<String, String> props;

    public M3uDoc(List<M3uChannel> channels, Map<String, String> props) {
        this.channels = channels;
        this.props = props;
    }

    public List<M3uChannel> getChannels() {
        return channels;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public String getProp(String key, String value) {
        return props.get(key);
    }
}
