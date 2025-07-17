package de.danoeh.antennapod.ui.cleaner;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link ShownotesCleaner}.
 */
@RunWith(RobolectricTestRunner.class)
public class ShownotesCleanerTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testProcessShownotesAddTimecodeHhmmssNoChapters() {
        final String timeStr = "10:11:12";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11 + 12 * 1000;

        String shownotes = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeHhmmssMoreThen24HoursNoChapters() {
        final String timeStr = "25:00:00";
        final long time = 25 * 60 * 60 * 1000;

        String shownotes = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeHhmmNoChapters() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        String shownotes = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeMmssNoChapters() {
        final String timeStr = "10:11";
        final long time = 10 * 60 * 1000 + 11 * 1000;

        String shownotes = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, 11 * 60 * 1000);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeHmmssNoChapters() {
        final String timeStr = "2:11:12";
        final long time = 2 * 60 * 60 * 1000 + 11 * 60 * 1000 + 12 * 1000;

        String shownotes = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeMssNoChapters() {
        final String timeStr = "1:12";
        final long time = 60 * 1000 + 12 * 1000;

        String shownotes = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, 2 * 60 * 1000);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddNoTimecodeDuration() {
        final String timeStr = "2:11:12";
        final int time = 2 * 60 * 60 * 1000 + 11 * 60 * 1000 + 12 * 1000;

        String shownotes = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, time);
        String res = t.processShownotes();
        Document d = Jsoup.parse(res);
        assertEquals("Should not parse time codes that equal duration", 0, d.body().getElementsByTag("a").size());
    }

    @Test
    public void testProcessShownotesAddTimecodeMultipleFormatsNoChapters() {
        final String[] timeStrings = new String[]{ "10:12", "1:10:12" };

        String shownotes = "<p> Some test text with a timecode " + timeStrings[0]
                + " here. Hey look another one " + timeStrings[1] + " here!</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, 2 * 60 * 60 * 1000);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{10 * 60 * 1000 + 12 * 1000,
                60 * 60 * 1000 + 10 * 60 * 1000 + 12 * 1000}, timeStrings);
    }

    @Test
    public void testProcessShownotesAddTimecodeMultipleShortFormatNoChapters() {

        // One of these timecodes fits as HH:MM and one does not so both should be parsed as MM:SS.
        final String[] timeStrings = new String[]{ "10:12", "2:12" };

        String shownotes = "<p> Some test text with a timecode " + timeStrings[0]
                + " here. Hey look another one " + timeStrings[1] + " here!</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, 3 * 60 * 60 * 1000);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{10 * 60 * 1000 + 12 * 1000, 2 * 60 * 1000 + 12 * 1000}, timeStrings);
    }

    @Test
    public void testProcessShownotesAddTimecodeParentheses() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        String shownotes = "<p> Some test text with a timecode (" + timeStr + ") here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeBrackets() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        String shownotes = "<p> Some test text with a timecode [" + timeStr + "] here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeAngleBrackets() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        String shownotes = "<p> Some test text with a timecode <" + timeStr + "> here.</p>";
        ShownotesCleaner t = new ShownotesCleaner(context, shownotes, Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAndInvalidTimecode()  {
        final String[] timeStrs = new String[] {"2:1", "0:0", "000", "00", "00:000"};

        StringBuilder shownotes = new StringBuilder("<p> Some test text with timecodes ");
        for (String timeStr : timeStrs) {
            shownotes.append(timeStr).append(" ");
        }
        shownotes.append("here.</p>");

        ShownotesCleaner t = new ShownotesCleaner(context, shownotes.toString(), Integer.MAX_VALUE);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[0], new String[0]);
    }

    private void checkLinkCorrect(String res, long[] timecodes, String[] timecodeStr) {
        assertNotNull(res);
        Document d = Jsoup.parse(res);
        Elements links = d.body().getElementsByTag("a");
        int countedLinks = 0;
        for (Element link : links) {
            String href = link.attributes().get("href");
            String text = link.text();
            if (href.startsWith("antennapod://")) {
                assertTrue(href.endsWith(String.valueOf(timecodes[countedLinks])));
                assertEquals(timecodeStr[countedLinks], text);
                countedLinks++;
                assertTrue("Contains too many links: " + countedLinks + " > "
                        + timecodes.length, countedLinks <= timecodes.length);
            }
        }
        assertEquals(timecodes.length, countedLinks);
    }

    @Test
    public void testIsTimecodeLink() {
        assertFalse(ShownotesCleaner.isTimecodeLink(null));
        assertFalse(ShownotesCleaner.isTimecodeLink("http://antennapod/timecode/123123"));
        assertFalse(ShownotesCleaner.isTimecodeLink("antennapod://timecode/"));
        assertFalse(ShownotesCleaner.isTimecodeLink("antennapod://123123"));
        assertFalse(ShownotesCleaner.isTimecodeLink("antennapod://timecode/123123a"));
        assertTrue(ShownotesCleaner.isTimecodeLink("antennapod://timecode/123"));
        assertTrue(ShownotesCleaner.isTimecodeLink("antennapod://timecode/1"));
    }

    @Test
    public void testGetTimecodeLinkTime() {
        assertEquals(-1, ShownotesCleaner.getTimecodeLinkTime(null));
        assertEquals(-1, ShownotesCleaner.getTimecodeLinkTime("http://timecode/123"));
        assertEquals(123, ShownotesCleaner.getTimecodeLinkTime("antennapod://timecode/123"));
    }

    @Test
    public void testCleanupColors() {
        final String input = "/* /* */ .foo { text-decoration: underline;color:#f00;font-weight:bold;}"
                + "#bar { text-decoration: underline;color:#f00;font-weight:bold; }"
                + "div {text-decoration: underline; color /* */ : /* */ #f00 /* */; font-weight:bold; }"
                + "#foobar { /* color: */ text-decoration: underline; /* color: */font-weight:bold /* ; */; }"
                + "baz { background-color:#f00;border: solid 2px;border-color:#0f0;text-decoration: underline; }";
        final String expected = " .foo { text-decoration: underline;font-weight:bold;}"
                + "#bar { text-decoration: underline;font-weight:bold; }"
                + "div {text-decoration: underline;  font-weight:bold; }"
                + "#foobar {  text-decoration: underline; font-weight:bold ; }"
                + "baz { background-color:#f00;border: solid 2px;border-color:#0f0;text-decoration: underline; }";
        assertEquals(expected, ShownotesCleaner.cleanStyleTag(input));
    }
}
