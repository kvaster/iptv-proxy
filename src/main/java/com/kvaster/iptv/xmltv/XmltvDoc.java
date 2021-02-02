package com.kvaster.iptv.xmltv;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "tv")
public class XmltvDoc {
    @JacksonXmlProperty(isAttribute = true, localName = "generator-info-name")
    private String generatorName;

    @JacksonXmlProperty(isAttribute = true, localName = "generator-info-url")
    private String generatorUrl;

    @JacksonXmlProperty(isAttribute = true, localName = "source-info-url")
    private String sourceInfoUrl;

    @JacksonXmlProperty(isAttribute = true, localName = "source-info-name")
    private String sourceInfoName;

    @JacksonXmlProperty(isAttribute = true, localName = "source-info-logo")
    private String sourceInfoLogo;

    @JacksonXmlProperty(localName = "channel")
    private List<XmltvChannel> channels;

    @JacksonXmlProperty(localName = "programme")
    private List<XmltvProgramme> programmes;

    public XmltvDoc() {
    }

    public XmltvDoc(List<XmltvChannel> channels, List<XmltvProgramme> programmes) {
        this.channels = channels;
        this.programmes = programmes;
    }

    public List<XmltvChannel> getChannels() {
        return channels;
    }

    public XmltvDoc setChannels(List<XmltvChannel> channels) {
        this.channels = channels;
        return this;
    }

    public List<XmltvProgramme> getProgrammes() {
        return programmes;
    }

    public XmltvDoc setProgrammes(List<XmltvProgramme> programmes) {
        this.programmes = programmes;
        return this;
    }

    public String getGeneratorName() {
        return generatorName;
    }

    public XmltvDoc setGeneratorName(String generatorName) {
        this.generatorName = generatorName;
        return this;
    }

    public String getGeneratorUrl() {
        return generatorUrl;
    }

    public XmltvDoc setGeneratorUrl(String generatorUrl) {
        this.generatorUrl = generatorUrl;
        return this;
    }

    public String getSourceInfoUrl() {
        return sourceInfoUrl;
    }

    public XmltvDoc setSourceInfoUrl(String sourceInfoUrl) {
        this.sourceInfoUrl = sourceInfoUrl;
        return this;
    }

    public String getSourceInfoName() {
        return sourceInfoName;
    }

    public XmltvDoc setSourceInfoName(String sourceInfoName) {
        this.sourceInfoName = sourceInfoName;
        return this;
    }

    public String getSourceInfoLogo() {
        return sourceInfoLogo;
    }

    public XmltvDoc setSourceInfoLogo(String sourceInfoLogo) {
        this.sourceInfoLogo = sourceInfoLogo;
        return this;
    }
}
