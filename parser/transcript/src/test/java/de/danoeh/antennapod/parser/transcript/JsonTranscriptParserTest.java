package de.danoeh.antennapod.parser.transcript;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import de.danoeh.antennapod.model.feed.Transcript;

@RunWith(RobolectricTestRunner.class)
public class JsonTranscriptParserTest {
    private static String jsonStr = "{'version': '1.0.0', "
            + "'segments': [ "
            + "{ 'speaker' : 'John Doe', 'startTime': 0.8, 'endTime': 1.9, 'body': 'And' },"
            + "{ 'speaker' : 'Sally Green', 'startTime': 1.91, 'endTime': 2.8, 'body': 'this merges' },"
            + "{ 'startTime': 2.9, 'endTime': 3.4, 'body': ' the' },"
            + "{ 'startTime': 3.5, 'endTime': 3.6, 'body': ' person' }]}";

    @Test
    public void testParseJson() {
        Transcript result = JsonTranscriptParser.parse(jsonStr);
        // TODO: for gaps in the transcript (ads, music) should we return null?
        assertEquals(result.getSegmentAtTime(0L).getStartTime(), 800L);
        assertEquals(result.getSegmentAtTime(800L).getSpeaker(), "John Doe");
        assertEquals(result.getSegmentAtTime(800L).getStartTime(), 800L);
        assertEquals(result.getSegmentAtTime(800L).getEndTime(), 1900L);
        assertEquals(result.getSegmentAtTime(1800L).getStartTime(), 800L);
        // 2 segments get merged into at least 5 second
        assertEquals(result.getSegmentAtTime(1800L).getWords(), "And");
    }

    @Test
    public void testParse() {
        String type = "application/json";
        Transcript result = TranscriptParser.parse(jsonStr, type);
        // There isn't a segment at 900L, so go backwards and get the segment at 800L
        assertEquals(result.getSegmentAtTime(900L).getSpeaker(), "John Doe");
        assertEquals(result.getSegmentAtTime(930L).getWords(), "And");

        // blank string
        String blankStr = "";
        result = TranscriptParser.parse(blankStr, type);
        assertEquals(result, null);

        result = TranscriptParser.parse(null, type);
        assertEquals(result, null);

        // All blank lines
        String allNewlinesStr = "\r\n\r\n\r\n\r\n";
        result = TranscriptParser.parse(allNewlinesStr, type);
        assertEquals(result, null);

        // segments is missing
        String jsonStrBad1 = "{'version': '1.0.0', "
                + "'segmentsX': [ "
                + "{ 'speaker' : 'John Doe', 'startTime': 0.8, 'endTime': 1.9, 'body': 'And' },"
                + "{ 'startTime': 2.9, 'endTime': 3.4, 'body': 'the' },"
                + "{ 'startTime': 3.5, 'endTime': 3.6, 'body': 'person' }]}";
        result = TranscriptParser.parse(jsonStrBad1, type);
        assertEquals(result, null);

        // invalid time formatting
        String jsonStrBad2 = "{'version': '1.0.0', "
                + "'segments': [ "
                + "{ 'speaker' : 'XJohn Doe', 'startTime': stringTime, 'endTime': stringTime, 'body': 'And' },"
                + "{ 'XstartTime': 2.9, 'XendTime': 3.4, 'body': 'the' },"
                + "{ 'startTime': '-2.9', 'endTime': '-3.4', 'body': 'the' },"
                + "{ 'startTime': 'bad_time', 'endTime': '-3.4', 'body': 'the' }]}";
        result = TranscriptParser.parse(jsonStrBad2, type);
        assertNull(result);

        // Just plain text
        String strBad3 = "John Doe: Promoting your podcast in a new\n\n"
                + "way. The latest from PogNews.";
        result = TranscriptParser.parse(strBad3, type);
        assertNull(result);

        // passing the wrong type
        type = "application/srt";
        result = TranscriptParser.parse(jsonStr, type);
        assertEquals(result, null);
    }
}
