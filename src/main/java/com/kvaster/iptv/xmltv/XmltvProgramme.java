package com.kvaster.iptv.xmltv;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class XmltvProgramme {
    @JacksonXmlProperty(isAttribute = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMddHHmmss Z")
    private ZonedDateTime start;

    @JacksonXmlProperty(isAttribute = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMddHHmmss Z")
    private ZonedDateTime stop;

    @JacksonXmlProperty(isAttribute = true)
    private String channel;

    private XmltvText category;

    private XmltvText title;

    private XmltvText desc;

    private XmltvRating rating;

    private XmltvIcon icon;

    public XmltvProgramme() {
    }

    public XmltvProgramme(String channel, ZonedDateTime start, ZonedDateTime stop) {
        this.channel = channel;
        this.start = start;
        this.stop = stop;
    }

    public String getChannel() {
        return channel;
    }

    public XmltvProgramme setChannel(String channel) {
        this.channel = channel;
        return this;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public XmltvProgramme setStart(ZonedDateTime start) {
        this.start = start;
        return this;
    }

    public ZonedDateTime getStop() {
        return stop;
    }

    public XmltvProgramme setStop(ZonedDateTime stop) {
        this.stop = stop;
        return this;
    }

    public XmltvText getCategory() {
        return category;
    }

    public XmltvProgramme setCategory(XmltvText category) {
        this.category = category;
        return this;
    }

    public XmltvText getTitle() {
        return title;
    }

    public XmltvProgramme setTitle(XmltvText title) {
        this.title = title;
        return this;
    }

    public XmltvText getDesc() {
        return desc;
    }

    public XmltvProgramme setDesc(XmltvText desc) {
        this.desc = desc;
        return this;
    }

    public XmltvRating getRating() {
        return rating;
    }

    public XmltvProgramme setRating(XmltvRating rating) {
        this.rating = rating;
        return this;
    }

    public XmltvIcon getIcon() {
        return icon;
    }

    public XmltvProgramme setIcon(XmltvIcon icon) {
        this.icon = icon;
        return this;
    }
}
