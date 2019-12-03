package de.danoeh.antennapodSA.core.syndication.namespace.atom;

import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link AtomText}.
 */
@SmallTest
@RunWith(Parameterized.class)
public class AtomTextTest {

    @Parameter(value = 0)
    public String input;

    @Parameter(value = 1)
    public String expectedOutput;

    @Parameters
    public static Collection<Object[]> initParameters() {
        return Arrays.asList(new Object[][] {
                {"&gt;", ">"},
                {">", ">"},
                {"&lt;Fran&ccedil;ais&gt;", "<Français>"},
                {"ßÄÖÜ", "ßÄÖÜ"},
                {"&quot;", "\""},
                {"&szlig;", "ß"},
                {"&#8217;", "’"},
                {"&#x2030;", "‰"},
                {"&euro;", "€"},
        });
    }

    @Test
    public void testProcessingHtml() {
        final AtomText atomText = new AtomText("", new NSAtom(), AtomText.TYPE_HTML);
        atomText.setContent(input);
        assertEquals(expectedOutput, atomText.getProcessedContent());
    }
}
