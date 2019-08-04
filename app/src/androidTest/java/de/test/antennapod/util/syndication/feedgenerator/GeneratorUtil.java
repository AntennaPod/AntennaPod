package de.test.antennapod.util.syndication.feedgenerator;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Utility methods for FeedGenerator
 */
class GeneratorUtil {
    private GeneratorUtil(){}

    public static void addPaymentLink(XmlSerializer xml, String paymentLink, boolean withNamespace) throws IOException {
        String ns = (withNamespace) ? "http://www.w3.org/2005/Atom" : null;
        xml.startTag(ns, "link");
        xml.attribute(null, "rel", "payment");
        xml.attribute(null, "href", paymentLink);
        xml.attribute(null, "type", "text/html");
        xml.endTag(ns, "link");
    }
}
