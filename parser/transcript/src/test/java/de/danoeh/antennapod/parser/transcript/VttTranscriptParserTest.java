package de.danoeh.antennapod.parser.transcript;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import de.danoeh.antennapod.model.feed.Transcript;

@RunWith(RobolectricTestRunner.class)
public class VttTranscriptParserTest {

    private static String vttStrSimple = """
        WEBVTT

        00:00.000 --> 00:02.000
        Intro without speaker

        00:00:02.000 --> 00:03.000
        Still no speaker

        00:03.389 --> 00:00:12.123
        <v Speaker 1>This is the first speaker.

        00:13.000 --> 00:15.000
        <v Speaker 2>And this is the second.

        00:15.000 --> 00:16.000
        <v Speaker 2>Still talking.

        00:16.000 --> 00:18.000
        <v Speaker 1>And again.
        Je suis speaker 1.

        01:00:00.000 --> 01:00:03.000
        <v Speaker 1>Still talking after one hour.""";

    // This is the same content as above, but with lots more WEBVTT features
    private static String vttStrComplex = """
        WEBVTT

        NOTE This is a note

        00:00.000 --> 00:02.000
        Intro without speaker

        00:00:02.000 --> 00:03.000
        Still no speaker

        NOTE Here is an other note

        First block with a speaker
        00:03.389 --> 00:00:12.123
        <v.first Speaker 1>This is the first speaker.

        00:13.000 --> 00:15.000\tposition:90% align:right size:35%
        <v.second.loud Speaker 2>And this is the second.</v>

        00:15.000 --> 00:16.000 position:10%,line-left align:left size:35%
        <v Speaker 2>Still talking.

        00:16.000 --> 00:18.000
        <v Speaker 1>And again.
        <i.foreignphrase><lang fr>Je suis</lang></i> speaker 1.

        After one hour
        01:00:00.000 --> 01:00:03.000
        <v Speaker 1>Still talking after one hour.""";

    private void checkResults(Transcript result) {
        assertEquals("Intro without speaker", result.getSegmentAtTime(0L).getWords());
        assertEquals("", result.getSegmentAtTime(0L).getSpeaker());
        assertEquals(2000L, result.getSegmentAtTime(0L).getEndTime());

        assertEquals("Still no speaker", result.getSegmentAtTime(2000L).getWords());
        assertEquals("", result.getSegmentAtTime(2000L).getSpeaker());
        assertEquals(3000L, result.getSegmentAtTime(2000L).getEndTime());

        assertEquals("This is the first speaker.", result.getSegmentAtTime(3389L).getWords());
        assertEquals("Speaker 1", result.getSegmentAtTime(3389L).getSpeaker());
        assertEquals(12123L, result.getSegmentAtTime(3389L).getEndTime());

        assertEquals("And this is the second.", result.getSegmentAtTime(13000L).getWords());
        assertEquals("Speaker 2", result.getSegmentAtTime(13000L).getSpeaker());
        assertEquals(15000L, result.getSegmentAtTime(13000L).getEndTime());

        assertEquals("Still talking.", result.getSegmentAtTime(15000L).getWords());
        assertEquals("Speaker 2", result.getSegmentAtTime(15000L).getSpeaker());
        assertEquals(16000L, result.getSegmentAtTime(15000L).getEndTime());

        assertEquals("And again. Je suis speaker 1.", result.getSegmentAtTime(16000L).getWords());
        assertEquals("Speaker 1", result.getSegmentAtTime(16000L).getSpeaker());
        assertEquals(18000L, result.getSegmentAtTime(16000L).getEndTime());

        assertEquals("Still talking after one hour.", result.getSegmentAtTime(10000000L).getWords());
        assertEquals("Speaker 1", result.getSegmentAtTime(3600000L).getSpeaker());
        assertEquals(3603000L, result.getSegmentAtTime(3600000L).getEndTime());
    }

    @Test
    public void testParseVttSimple() {
        Transcript result = VttTranscriptParser.parse(vttStrSimple);
        checkResults(result);
    }

    @Test
    public void testParseVttComplex() {
        Transcript result = VttTranscriptParser.parse(vttStrComplex);
        checkResults(result);
    }

    @Test
    public void testParse() {
        String type = "text/vtt";
        Transcript result;

        result = TranscriptParser.parse(vttStrSimple, type);
        // There isn't a segment at 800L, so go backwards and get the segment at 0L
        assertEquals("Intro without speaker", result.getSegmentAtTime(800L).getWords());

        result = TranscriptParser.parse(null, type);
        assertNull(result);

        // blank string
        result = TranscriptParser.parse("", type);
        assertNull(result);

        // All empty lines
        result = TranscriptParser.parse("\r\n\r\n\r\n\r\n", type);
        assertNull(result);

        // Just plain text
        result = TranscriptParser.parse("<v Speaker 1> Just text", type);
        assertNull(result);

        // passing the wrong type
        result = TranscriptParser.parse(vttStrSimple, "application/srr");
        assertNull(result);
        result = TranscriptParser.parse(vttStrSimple, "unknown");
        assertNull(result);
    }
}

