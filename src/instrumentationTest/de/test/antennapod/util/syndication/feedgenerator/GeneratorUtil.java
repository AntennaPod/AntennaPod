package instrumentationTest.de.test.antennapod.util.syndication.feedgenerator;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Utility methods for FeedGenerator
 */
public class GeneratorUtil {

    public static void addPaymentLink(XmlSerializer xml, String paymentLink) throws IOException {
        xml.startTag("http://www.w3.org/2005/Atom", "link");
        xml.attribute(null, "rel", "payment");
        xml.attribute(null, "title", "Flattr this!");
        xml.attribute(null, "href", paymentLink);
        xml.attribute(null, "type", "text/html");
        xml.endTag("http://www.w3.org/2005/Atom", "link");
    }
}
