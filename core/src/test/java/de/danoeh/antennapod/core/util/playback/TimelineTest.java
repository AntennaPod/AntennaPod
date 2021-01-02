package de.danoeh.antennapod.core.util.playback;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link Timeline}.
 */
@RunWith(RobolectricTestRunner.class)
public class TimelineTest {

    private Context context;
    MockedStatic<DBReader> dbReaderMock;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // mock DBReader, because Timeline.processShownotes() calls FeedItem.loadShownotes()
        // which calls DBReader.loadDescriptionOfFeedItem(), but we don't need the database here
        dbReaderMock = Mockito.mockStatic(DBReader.class);
    }

    @After
    public void tearDown() {
        dbReaderMock.close();
    }

    @SuppressWarnings("SameParameterValue")
    private Playable newTestPlayable(List<Chapter> chapters, String shownotes, int duration) {
        FeedItem item = new FeedItem(0, "Item", "item-id", "http://example.com/item", new Date(), FeedItem.PLAYED, null);
        item.setChapters(chapters);
        item.setContentEncoded(shownotes);
        FeedMedia media = new FeedMedia(item, "http://example.com/episode", 100, "audio/mp3");
        media.setDuration(duration);
        item.setMedia(media);
        return media;
    }

    @Test
    public void testProcessShownotesAddTimecodeHhmmssNoChapters() {
        final String timeStr = "10:11:12";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11 + 12 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeHhmmssMoreThen24HoursNoChapters() {
        final String timeStr = "25:00:00";
        final long time = 25 * 60 * 60 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeHhmmNoChapters() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeMmssNoChapters() {
        final String timeStr = "10:11";
        final long time = 10 * 60 * 1000 + 11 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStr + " here.</p>", 11 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeHmmssNoChapters() {
        final String timeStr = "2:11:12";
        final long time = 2 * 60 * 60 * 1000 + 11 * 60 * 1000 + 12 * 1000;
        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeMssNoChapters() {
        final String timeStr = "1:12";
        final long time = 60 * 1000 + 12 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStr + " here.</p>", 2 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddNoTimecodeDuration() {
        final String timeStr = "2:11:12";
        final int time = 2 * 60 * 60 * 1000 + 11 * 60 * 1000 + 12 * 1000;
        String originalText = "<p> Some test text with a timecode " + timeStr + " here.</p>";
        Playable p = newTestPlayable(null, originalText, time);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        Document d = Jsoup.parse(res);
        assertEquals("Should not parse time codes that equal duration", 0, d.body().getElementsByTag("a").size());
    }

    @Test
    public void testProcessShownotesAddTimecodeMultipleFormatsNoChapters() {
        final String[] timeStrings = new String[]{ "10:12", "1:10:12" };

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStrings[0] + " here. Hey look another one " + timeStrings[1] + " here!</p>", 2 * 60 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{10 * 60 * 1000 + 12 * 1000,
                60 * 60 * 1000 + 10 * 60 * 1000 + 12 * 1000}, timeStrings);
    }

    @Test
    public void testProcessShownotesAddTimecodeMultipleShortFormatNoChapters() {

        // One of these timecodes fits as HH:MM and one does not so both should be parsed as MM:SS.
        final String[] timeStrings = new String[]{ "10:12", "2:12" };

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode "
                + timeStrings[0] + " here. Hey look another one " + timeStrings[1] + " here!</p>", 3 * 60 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{10 * 60 * 1000 + 12 * 1000, 2 * 60 * 1000 + 12 * 1000}, timeStrings);
    }

    @Test
    public void testProcessShownotesAddTimecodeParentheses() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode ("
                + timeStr + ") here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeBrackets() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode ["
                + timeStr + "] here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes();
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    @Test
    public void testProcessShownotesAddTimecodeAngleBrackets() {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode <"
                + timeStr + "> here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
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

        Playable p = newTestPlayable(null, shownotes.toString(), Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
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
        assertFalse(Timeline.isTimecodeLink(null));
        assertFalse(Timeline.isTimecodeLink("http://antennapod/timecode/123123"));
        assertFalse(Timeline.isTimecodeLink("antennapod://timecode/"));
        assertFalse(Timeline.isTimecodeLink("antennapod://123123"));
        assertFalse(Timeline.isTimecodeLink("antennapod://timecode/123123a"));
        assertTrue(Timeline.isTimecodeLink("antennapod://timecode/123"));
        assertTrue(Timeline.isTimecodeLink("antennapod://timecode/1"));
    }

    @Test
    public void testGetTimecodeLinkTime() {
        assertEquals(-1, Timeline.getTimecodeLinkTime(null));
        assertEquals(-1, Timeline.getTimecodeLinkTime("http://timecode/123"));
        assertEquals(123, Timeline.getTimecodeLinkTime("antennapod://timecode/123"));

    }
}
