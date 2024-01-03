package de.danoeh.antennapod.parser.transcript;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import de.danoeh.antennapod.parser.transcript.PodcastIndexTranscriptParser;
import de.danoeh.antennapod.model.feed.Transcript;

@RunWith(RobolectricTestRunner.class)
public class PodcastIndexTranscriptParserTest {
    private static String srtStr = "1\n"
            + "00:00:00,000 --> 00:00:02,730\n"
            + "John Doe: Promoting your podcast in a new\n\n"
            + "2\n"
            + "00:00:02,730 --> 00:00:04,600\n"
            + "way. The latest from PogNews.";
    private static String jsonStr = "{'version': '1.0.0', "
            + "'segments': [ "
            + "{ 'speaker' : 'John Doe', 'startTime': 0.8, 'endTime': 1.9, 'body': 'And' },"
            + "{ 'speaker' : 'Sally Green', 'startTime': 1.91, 'endTime': 2.8, 'body': 'this merges' },"
            + "{ 'startTime': 2.9, 'endTime': 3.4, 'body': 'the' }]}";

    // segments is missing
    private static String jsonStrBad1 = "{'version': '1.0.0', "
            + "'segmentsX': [ "
            + "{ 'speaker' : 'John Doe', 'startTime': 0.8, 'endTime': 1.9, 'body': 'And' },"
            + "{ 'startTime': 2.9, 'endTime': 3.4, 'body': 'the' }]}";

    // invalid time formatting
    private static String jsonStrBad2 = "{'version': '1.0.0', "
            + "'segments': [ "
            + "{ 'speaker' : 'John Doe', 'startTime': stringTime, 'endTime': stringTime, 'body': 'And' },"
            + "{ 'XstartTime': 2.9, 'XendTime': 3.4, 'body': 'the' }]}";

    // blank string
    private static String blankStr = "";

    // All blank lines
    private static String allNewlinesStr = "\r\n\r\n\r\n\r\n";

    // first segment has invalid time formatting, so the entire segment will be thrown out
    private static String srtStrBad1 = "00:0000,000 --> 00:00:02,730\n"
            + "John Doe: Promoting your podcast in a new\n\n"
            + "2\n"
            + "00:00:02,730 --> 00:00:04,600\n"
            + "way. The latest from PogNews.";

    // first segment has invalid time in end time, 2nd segment has invalid time in both start time and end time
    private static String srtStrBad2 = "00:00:00,000 --> 00:0002,730\n"
            + "John Doe: Promoting your podcast in a new\n\n"
            + "2\n"
            + "badstarttime --> badendtime\n"
            + "way. The latest from PogNews.";

    // Just plain text
    private static String strBad3 = "John Doe: Promoting your podcast in a new\n\n"
            + "way. The latest from PogNews.";

    @Test
    public void testParseJson() {
        Transcript result = PodcastIndexJsonTranscriptParser.parse(jsonStr);

        assertEquals(result.getSegmentAtTime(0L), null);
        assertEquals(result.getSegmentAtTime(800L).getSpeaker(), "John Doe");
        assertEquals(result.getSegmentAtTime(800L).getStartTime(), 800L);
        assertEquals(result.getSegmentAtTime(800L).getEndTime(), 1900L);
        assertEquals(1910L, (long) result.getEntryAfterTime(1800L).getKey());
        // 2 segments get merged into at least 1 second
        assertEquals("this merges the", result.getEntryAfterTime(1800L).getValue().getWords());
    }

    @Test
    public void testParseSrt() {
        Transcript result = SrtTranscriptParser.parse(srtStr);

        assertEquals(result.getSegmentAtTime(0L).getWords(), "Promoting your podcast in a new");
        assertEquals(result.getSegmentAtTime(0L).getSpeaker(), "John Doe");
        assertEquals(result.getSegmentAtTime(0L).getStartTime(), 0L);
        assertEquals(result.getSegmentAtTime(0L).getEndTime(), 2730L);
        assertEquals((long) result.getEntryAfterTime(1000L).getKey(), 2730L);
        assertEquals(result.getEntryAfterTime(1000L).getValue().getWords(), "way. The latest from PogNews.");
    }

    @Test
    public void testParse() {
        String type = "application/json";
        Transcript result = PodcastIndexTranscriptParser.parse(jsonStr, type);
        assertEquals(result.getSegmentAtTime(800L).getSpeaker(), "John Doe");
        assertEquals(result.getSegmentAtTime(800L).getWords(), "And");

        result = PodcastIndexTranscriptParser.parse(blankStr, type);
        assertEquals(result, null);

        result = PodcastIndexTranscriptParser.parse(null, type);
        assertEquals(result, null);

        result = PodcastIndexTranscriptParser.parse(allNewlinesStr, type);
        assertEquals(result, null);

        result = PodcastIndexTranscriptParser.parse(jsonStrBad1, type);
        assertEquals(result, null);

        result = PodcastIndexTranscriptParser.parse(jsonStrBad2, type);
        assertNull(result);

        result = PodcastIndexTranscriptParser.parse(strBad3, type);
        assertNull(result);

        type = "application/srr";
        result = PodcastIndexTranscriptParser.parse(srtStr, type);
        assertEquals(result.getSegmentAtTime(800L).getWords(), "Promoting your podcast in a new");

        result = PodcastIndexTranscriptParser.parse(null, type);
        assertEquals(result, null);

        result = PodcastIndexTranscriptParser.parse(blankStr, type);
        assertNull(result);

        result = PodcastIndexTranscriptParser.parse(allNewlinesStr, type);
        assertEquals(result, null);

        result = PodcastIndexTranscriptParser.parse(srtStrBad1, type);
        assertEquals(result.getSegmentAtTime(2730L).getWords(), "way. The latest from PogNews.");

        result = PodcastIndexTranscriptParser.parse(srtStrBad2, type);
        assertNull(result);

        result = PodcastIndexTranscriptParser.parse(strBad3, type);
        assertNull(result);

        // passing the wrong type
        type = "application/srt";
        result = PodcastIndexTranscriptParser.parse(jsonStr, type);
        assertEquals(result, null);

        // passing the wrong type
        type = "application/json";
        result = PodcastIndexTranscriptParser.parse(srtStr, type);
        assertEquals(result, null);
    }
}
