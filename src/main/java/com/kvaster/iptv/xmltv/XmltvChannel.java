package com.kvaster.iptv.xmltv;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XmltvChannel {
    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "display-name")
    private List<XmltvText> displayNames;

    private XmltvIcon icon;

    public XmltvChannel() {
    }

    public XmltvChannel(String id, List<XmltvText> displayNames, XmltvIcon icon) {
        this.id = id;
        this.displayNames = displayNames;
        this.icon = icon;
    }

    public XmltvChannel(String id, XmltvText displayName, XmltvIcon icon) {
        this(id, Collections.singletonList(displayName), icon);
    }

    public String getId() {
        return id;
    }

    public XmltvChannel setId(String id) {
        this.id = id;
        return this;
    }

    public List<XmltvText> getDisplayNames() {
        return displayNames;
    }

    public XmltvChannel setDisplayNames(List<XmltvText> displayNames) {
        this.displayNames = displayNames;
        return this;
    }

    public XmltvIcon getIcon() {
        return icon;
    }

    public XmltvChannel setIcon(XmltvIcon icon) {
        this.icon = icon;
        return this;
    }
}
