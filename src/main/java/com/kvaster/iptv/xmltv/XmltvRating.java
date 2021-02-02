package com.kvaster.iptv.xmltv;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XmltvRating {
    @JacksonXmlProperty(isAttribute = true)
    private String system;

    private String value;

    public XmltvRating() {
    }

    public XmltvRating(String system, String value) {
        this.system = system;
        this.value = value;
    }

    public String getSystem() {
        return system;
    }

    public XmltvRating setSystem(String system) {
        this.system = system;
        return this;
    }

    public XmltvRating setValue(String value) {
        this.value = value;
        return this;
    }
}
