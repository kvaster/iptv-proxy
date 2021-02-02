package com.kvaster.iptv.xmltv;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XmltvIcon {
    @JacksonXmlProperty(isAttribute = true)
    String src;

    public XmltvIcon() {
    }

    public XmltvIcon(String src) {
        this.src = src;
    }

    public String getSrc() {
        return src;
    }

    public XmltvIcon setSrc(String src) {
        this.src = src;
        return this;
    }
}
