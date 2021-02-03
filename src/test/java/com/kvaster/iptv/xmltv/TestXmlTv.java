package com.kvaster.iptv.xmltv;

import java.io.File;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestXmlTv {
    private static final Logger LOG = LoggerFactory.getLogger(TestXmlTv.class);

    public static void main(String[] args) {
        try {
            XmlMapper xm = XmltvUtils.createMapper();

            XmltvDoc doc = xm.readValue(new File("/home/kva/projects/kvaster/iptv/epg-cbilling.xml"), XmltvDoc.class);
            //doc = xm.readValue(new File("/home/kva/projects/kvaster/iptv/epg-crdru.xml"), XmltvDoc.class);
            doc = xm.readValue(new File("/home/kva/projects/kvaster/iptv/epg-ilooktv.xml"), XmltvDoc.class);
            //xm.writerWithDefaultPrettyPrinter().writeValue(new File("out.xml"), doc);
            xm.writeValue(new File("tmp/out.xml"), doc);
            LOG.info("done");
        } catch (Exception e) {
            LOG.error("error", e);
        }
    }
}
