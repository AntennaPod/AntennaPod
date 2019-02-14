package de.test.antennapod.util.playback;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Date;
import java.util.List;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.Timeline;

/**
 * Test class for timeline
 */
public class TimelineTest extends InstrumentationTestCase {

    private Context context;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getTargetContext();
    }

    private Playable newTestPlayable(List<Chapter> chapters, String shownotes, int duration) {
        FeedItem item = new FeedItem(0, "Item", "item-id", "http://example.com/item", new Date(), FeedItem.PLAYED, null);
        item.setChapters(chapters);
        item.setContentEncoded(shownotes);
        FeedMedia media = new FeedMedia(item, "http://example.com/episode", 100, "audio/mp3");
        media.setDuration(duration);
        item.setMedia(media);
        return media;
    }

    public void testProcessShownotesAddTimecodeHHMMSSNoChapters() throws Exception {
        final String timeStr = "10:11:12";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11 + 12 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeHHMMSSMoreThen24HoursNoChapters() throws Exception {
        final String timeStr = "25:00:00";
        final long time = 25 * 60 * 60 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeHHMMNoChapters() throws Exception {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeMMSSNoChapters() throws Exception {
        final String timeStr = "10:11";
        final long time = 10 * 60 * 1000 + 11 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStr + " here.</p>", 11 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeHMMSSNoChapters() throws Exception {
        final String timeStr = "2:11:12";
        final long time = 2 * 60 * 60 * 1000 + 11 * 60 * 1000 + 12 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStr + " here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeMSSNoChapters() throws Exception {
        final String timeStr = "1:12";
        final long time = 60 * 1000 + 12 * 1000;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStr + " here.</p>", 2 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeMultipleFormatsNoChapters() throws Exception {
        final String[] timeStrings = new String[]{ "10:12", "1:10:12" };

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStrings[0] + " here. Hey look another one " + timeStrings[1] + " here!</p>", 2 * 60 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{ 10 * 60 * 1000 + 12 * 1000, 60 * 60 * 1000 + 10 * 60 * 1000 + 12 * 1000 }, timeStrings);
    }

    public void testProcessShownotesAddTimecodeMultipleShortFormatNoChapters() throws Exception {

        // One of these timecodes fits as HH:MM and one does not so both should be parsed as MM:SS.
        final String[] timeStrings = new String[]{ "10:12", "2:12" };

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode " + timeStrings[0] + " here. Hey look another one " + timeStrings[1] + " here!</p>", 3 * 60 * 60 * 1000);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{ 10 * 60 * 1000 + 12 * 1000, 2 * 60 * 1000 + 12 * 1000 }, timeStrings);
    }

    public void testProcessShownotesAddTimecodeParentheses() throws Exception {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode (" + timeStr + ") here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeBrackets() throws Exception {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode [" + timeStr + "] here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
    }

    public void testProcessShownotesAddTimecodeAngleBrackets() throws Exception {
        final String timeStr = "10:11";
        final long time = 3600 * 1000 * 10 + 60 * 1000 * 11;

        Playable p = newTestPlayable(null, "<p> Some test text with a timecode <" + timeStr + "> here.</p>", Integer.MAX_VALUE);
        Timeline t = new Timeline(context, p);
        String res = t.processShownotes(true);
        checkLinkCorrect(res, new long[]{time}, new String[]{timeStr});
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
                assertTrue("Contains too many links: " + countedLinks + " > " + timecodes.length, countedLinks <= timecodes.length);
            }
        }
        assertEquals(timecodes.length, countedLinks);
    }

    public void testIsTimecodeLink() throws Exception {
        assertFalse(Timeline.isTimecodeLink(null));
        assertFalse(Timeline.isTimecodeLink("http://antennapod/timecode/123123"));
        assertFalse(Timeline.isTimecodeLink("antennapod://timecode/"));
        assertFalse(Timeline.isTimecodeLink("antennapod://123123"));
        assertFalse(Timeline.isTimecodeLink("antennapod://timecode/123123a"));
        assertTrue(Timeline.isTimecodeLink("antennapod://timecode/123"));
        assertTrue(Timeline.isTimecodeLink("antennapod://timecode/1"));
    }

    public void testGetTimecodeLinkTime() throws Exception {
        assertEquals(-1, Timeline.getTimecodeLinkTime(null));
        assertEquals(-1, Timeline.getTimecodeLinkTime("http://timecode/123"));
        assertEquals(123, Timeline.getTimecodeLinkTime("antennapod://timecode/123"));

    }
}
