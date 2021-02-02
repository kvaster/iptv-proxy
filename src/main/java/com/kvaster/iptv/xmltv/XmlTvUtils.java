package com.kvaster.iptv.xmltv;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class XmlTvUtils {
    public static XmlMapper createMapper() {
        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        XmlMapper xm = new XmlMapper(xmlModule);
        xm.registerModule(new JavaTimeModule());
        xm.setVisibility(new VisibilityChecker.Std(JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.ANY, JsonAutoDetect.Visibility.ANY));
        xm.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        xm.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
        xm.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false);
        xm.configure(MapperFeature.AUTO_DETECT_SETTERS, false);
        xm.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        return xm;
    }
}
