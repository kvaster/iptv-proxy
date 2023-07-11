package com.kvaster.iptv.xmltv;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmltvUtils {
    private static final Logger LOG = LoggerFactory.getLogger(XmltvUtils.class);

    public static final XmlMapper xmltvMapper = createMapper();

    public static XmlMapper createMapper() {
        return XmlMapper.builder()
                .configure(MapperFeature.AUTO_DETECT_GETTERS, false)
                .configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
                .configure(MapperFeature.AUTO_DETECT_SETTERS, false)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .defaultUseWrapper(false)
                .addModule(new JavaTimeModule())
                .visibility(new VisibilityChecker.Std(JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.NONE, JsonAutoDetect.Visibility.ANY, JsonAutoDetect.Visibility.ANY))
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }

    public static XmltvDoc parseXmltv(byte[] data) {
        try {
            try (InputStream is = openStream(data)) {
                return xmltvMapper.readValue(is, XmltvDoc.class);
            }
        } catch (IOException e) {
            LOG.error("error parsing xmltv data");
            return null;
        }
    }

    private static InputStream openStream(byte[] data) throws IOException {
        InputStream is = new ByteArrayInputStream(data);
        if (data.length >= 2 && data[0] == (byte)0x1f && data[1] == (byte)0x8b) {
            is = new GZIPInputStream(is);
        }

        return is;
    }

    public static byte[] writeXmltv(XmltvDoc xmltv) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try (GZIPOutputStream gos = new GZIPOutputStream(bos); BufferedOutputStream bbos = new BufferedOutputStream(gos)) {
                bbos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE tv SYSTEM \"xmltv.dtd\">\n".getBytes(StandardCharsets.UTF_8));
                xmltvMapper.writeValue(bbos, xmltv);
                //xmltvMapper.writerWithDefaultPrettyPrinter().writeValue(bbos, xmltv);
                bbos.flush();
            }

            return bos.toByteArray();
        } catch (IOException e) {
            LOG.error("error serializing xmltv data", e);
            throw new RuntimeException(e);
        }
    }
}
