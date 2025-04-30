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

        00:03.389 --> 00:00:09.000
        <v Speaker 1>This is the first speaker.

        00:09.150 --> 00:12.123
        Let's assume it's still the first speaker.

        00:13.000 --> 00:15.000
        <v Speaker 2>And this is the second.

        00:15.000 --> 00:16.000
        <v Speaker 2>Still talking.

        00:16.000 --> 00:18.000
        <v Speaker 2>Still same line.

        00:18.000 --> 00:19.000
        <v Speaker 2>New line.

        00:22.000 --> 00:26.500
        <v Speaker 2> Too long to collapse with previous.

        00:36.000 --> 00:38.000
        <v Speaker 1>And again.
        Je suis speaker 1.

        01:00:00.000 --> 01:00:01.000
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
        00:03.389 --> 00:00:09.000
        <v.first Speaker 1>This is the first speaker.

        00:09.150 --> 00:12.123
        Let's assume it's still the first speaker.

        00:13.000 --> 00:15.000\tposition:90% align:right size:35%
        <v.second.loud Speaker 2>And this is the second.</v>

        00:15.000 --> 00:16.000 position:10%,line-left align:left size:35%
        <v Speaker 2>Still talking.

        00:16.000 --> 00:18.000
        <v Speaker 2>Still same line.

        00:18.000 --> 00:19.000
        <v Speaker 2>New line.

        00:22.000 --> 00:26.500
        <v Speaker 2> Too long to collapse with previous.

        00:36.000 --> 00:38.000
        <v Speaker 1>And again.
        <i.foreignphrase><lang fr>Je suis</lang></i> speaker 1.

        After one hour
        01:00:00.000 --> 01:00:01.000
        <v Speaker 1>Still talking after one hour.""";

    private void checkResults(Transcript result) {
        assertEquals("Intro without speaker Still no speaker", result.getSegmentAtTime(0L).getWords());
        assertEquals("", result.getSegmentAtTime(0L).getSpeaker());
        assertEquals(3000L, result.getSegmentAtTime(0L).getEndTime());

        assertEquals("This is the first speaker.", result.getSegmentAtTime(3389L).getWords());
        assertEquals("Speaker 1", result.getSegmentAtTime(3389L).getSpeaker());
        assertEquals(9000L, result.getSegmentAtTime(3389L).getEndTime());

        assertEquals("Let's assume it's still the first speaker.", result.getSegmentAtTime(9150L).getWords());
        assertEquals("Speaker 1", result.getSegmentAtTime(9150L).getSpeaker());
        assertEquals(12123L, result.getSegmentAtTime(9150L).getEndTime());

        assertEquals("And this is the second. Still talking. Still same line.",
                result.getSegmentAtTime(13000L).getWords());
        assertEquals("Speaker 2", result.getSegmentAtTime(13000L).getSpeaker());
        assertEquals(18000L, result.getSegmentAtTime(13000L).getEndTime());

        assertEquals("New line.", result.getSegmentAtTime(18000L).getWords());
        assertEquals("Speaker 2", result.getSegmentAtTime(18000L).getSpeaker());
        assertEquals(19000L, result.getSegmentAtTime(18000L).getEndTime());

        assertEquals("Too long to collapse with previous.", result.getSegmentAtTime(22000L).getWords());
        assertEquals("Speaker 2", result.getSegmentAtTime(22000L).getSpeaker());
        assertEquals(26500L, result.getSegmentAtTime(22000L).getEndTime());

        assertEquals("And again. Je suis speaker 1.", result.getSegmentAtTime(36000L).getWords());
        assertEquals("Speaker 1", result.getSegmentAtTime(36000L).getSpeaker());
        assertEquals(38000L, result.getSegmentAtTime(36000L).getEndTime());

        assertEquals("Still talking after one hour.", result.getSegmentAtTime(10000000L).getWords());
        assertEquals("Speaker 1", result.getSegmentAtTime(3600000L).getSpeaker());
        assertEquals(3601000L, result.getSegmentAtTime(3600000L).getEndTime());
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
        assertEquals("Intro without speaker Still no speaker", result.getSegmentAtTime(800L).getWords());

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

