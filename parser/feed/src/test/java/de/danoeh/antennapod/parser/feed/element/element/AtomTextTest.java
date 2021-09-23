package de.danoeh.antennapod.parser.feed.element.element;

import de.danoeh.antennapod.parser.feed.element.AtomText;
import de.danoeh.antennapod.parser.feed.namespace.Atom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link AtomText}.
 */
@RunWith(RobolectricTestRunner.class)
public class AtomTextTest {

    private static final String[][] TEST_DATA = {
            {"&gt;", ">"},
            {">", ">"},
            {"&lt;Fran&ccedil;ais&gt;", "<Français>"},
            {"ßÄÖÜ", "ßÄÖÜ"},
            {"&quot;", "\""},
            {"&szlig;", "ß"},
            {"&#8217;", "’"},
            {"&#x2030;", "‰"},
            {"&euro;", "€"}
    };

    @Test
    public void testProcessingHtml() {
        for (String[] pair : TEST_DATA) {
            final AtomText atomText = new AtomText("", new Atom(), AtomText.TYPE_HTML);
            atomText.setContent(pair[0]);
            assertEquals(pair[1], atomText.getProcessedContent());
        }
    }
}
