package de.danoeh.antennapod.parser.feed.element;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.assertEquals;

import de.danoeh.antennapod.parser.feed.PodcastIndexTranscriptParser;
import de.danoeh.antennapod.model.feed.Transcript;

@RunWith(RobolectricTestRunner.class)
public class PodcastIndexTranscriptParserTest {
    private static String srtStr = "1\n"
            + "00:00:00,000 --> 00:00:02,730\n"
            + "John Doe: Promoting your podcast in a new\n\n\n"
            + "00:00:02,730 --> 00:00:04,600\n"
            + "way. The latest from PogNews.";
    private static String jsonStr = "{'version': '1.0.0', "
            + "'segments': [ "
            + "{ 'speaker' : 'John Doe', 'startTime': 0.8, 'endTime': 1.9, 'body': 'And' },"
            + "{ 'startTime': 2.9, 'endTime': 3.4, 'body': 'the' }]}";

    @Test
    public void testParseJson() {
        Transcript result = PodcastIndexTranscriptParser.PodcastIndexTranscriptJsonParser.parse(jsonStr);

        assertEquals(result.getSegmentAtTime(0L), null);
        assertEquals(result.getSegmentAtTime(800L).getSpeaker(), "John Doe");
        assertEquals(result.getSegmentAtTime(800L).getStartTime(), 800L);
        assertEquals(result.getSegmentAtTime(800L).getEndTime(), 1900L);
        assertEquals((long) result.getEntryAfterTime(1800L).getKey(), 2900L);
        assertEquals(result.getEntryAfterTime(1800L).getValue().getWords(), "the");
    }

    @Test
    public void testParseSrt() {
        Transcript result = PodcastIndexTranscriptParser.PodcastIndexTranscriptSrtParser.parse(srtStr);

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

        type = "application/srr";
        result = PodcastIndexTranscriptParser.parse(srtStr, type);
        assertEquals(result.getSegmentAtTime(800L).getWords(), "Promoting your podcast in a new");

        // negative testing
        type = "application/srt";
        result = PodcastIndexTranscriptParser.parse(jsonStr, type);
        assertEquals(result, null);

        type = "application/json";
        result = PodcastIndexTranscriptParser.parse(srtStr, type);
        assertEquals(result, null);
    }
}
