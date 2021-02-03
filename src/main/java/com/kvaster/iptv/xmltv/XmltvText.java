package com.kvaster.iptv.xmltv;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class XmltvText {
    @JacksonXmlProperty(isAttribute = true, localName = "lang")
    private String language;

    @JacksonXmlText
    private String text;

    public XmltvText() {
    }

    public XmltvText(String text) {
        this(text, null);
    }

    public XmltvText(String text, String language) {
        this.text = text;
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public XmltvText setText(String text) {
        this.text = text;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public XmltvText setLanguage(String language) {
        this.language = language;
        return this;
    }
}
