package de.danoeh.antennapod.net.ai.service.ad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.model.ad.AdSegment;

/**
 * Unit tests for the logic in {@link AdAnalysisWorker}.
 * Tests the parsing, merging, and utility methods without requiring actual Workers or API calls.
 */
public class AdAnalysisWorkerTest {

    // ==================== JSON Sanitization Tests ====================

    @Test
    public void testSanitizeJson_plainJson() {
        String raw = "{\"ads\":[]}";
        String sanitized = sanitizeJson(raw);
        assertEquals("{\"ads\":[]}", sanitized);
    }

    @Test
    public void testSanitizeJson_withMarkdownCodeBlock() {
        String raw = "```json\n{\"ads\":[]}\n```";
        String sanitized = sanitizeJson(raw);
        assertEquals("{\"ads\":[]}", sanitized);
    }

    @Test
    public void testSanitizeJson_withMarkdownCodeBlockNoNewline() {
        String raw = "```{\"ads\":[]}```";
        String sanitized = sanitizeJson(raw);
        assertEquals("{\"ads\":[]}", sanitized);
    }

    @Test
    public void testSanitizeJson_withLeadingText() {
        String raw = "Here is the JSON: {\"ads\":[]}";
        String sanitized = sanitizeJson(raw);
        assertEquals("{\"ads\":[]}", sanitized);
    }

    @Test
    public void testSanitizeJson_withTrailingText() {
        String raw = "{\"ads\":[]} That's all.";
        String sanitized = sanitizeJson(raw);
        assertEquals("{\"ads\":[]}", sanitized);
    }

    @Test
    public void testSanitizeJson_withWhitespace() {
        String raw = "  \n  {\"ads\":[]}  \n  ";
        String sanitized = sanitizeJson(raw);
        assertEquals("{\"ads\":[]}", sanitized);
    }

    @Test
    public void testSanitizeJson_emptyInput() {
        String sanitized = sanitizeJson("");
        assertTrue(sanitized == null || sanitized.isEmpty());
    }

    @Test
    public void testSanitizeJson_noJsonObject() {
        String raw = "No JSON here";
        String sanitized = sanitizeJson(raw);
        // Should return cleaned string
        assertNotNull(sanitized);
    }

    @Test
    public void testSanitizeJson_complexMarkdown() {
        String raw = "```json\n{\n  \"ads\": [\n    {\"startSeconds\": 0, \"endSeconds\": 30}\n  ]\n}\n```";
        String sanitized = sanitizeJson(raw);
        assertTrue(sanitized.startsWith("{"));
        assertTrue(sanitized.endsWith("}"));
        assertTrue(sanitized.contains("ads"));
    }

    // ==================== Segment Parsing Tests ====================

    @Test
    public void testParseSegments_validJson() {
        String json = "{\"ads\":[{\"startSeconds\":10,\"endSeconds\":40,\"reason\":\"sponsor\",\"confidence\":0.9}]}";
        List<AdSegment> segments = parseSegments(json);

        assertEquals(1, segments.size());
        AdSegment segment = segments.get(0);
        assertEquals(10.0, segment.getStartSeconds(), 0.001);
        assertEquals(40.0, segment.getEndSeconds(), 0.001);
        assertEquals("sponsor", segment.getReason());
        assertEquals(0.9, segment.getConfidence(), 0.001);
    }

    @Test
    public void testParseSegments_multipleSegments() {
        String json = "{\"ads\":["
                + "{\"startSeconds\":0,\"endSeconds\":30,\"reason\":\"pre-roll\",\"confidence\":0.95},"
                + "{\"startSeconds\":600,\"endSeconds\":660,\"reason\":\"mid-roll\",\"confidence\":0.88},"
                + "{\"startSeconds\":1800,\"endSeconds\":1830,\"reason\":\"post-roll\",\"confidence\":0.92}"
                + "]}";
        List<AdSegment> segments = parseSegments(json);

        assertEquals(3, segments.size());
        assertEquals(0.0, segments.get(0).getStartSeconds(), 0.001);
        assertEquals(600.0, segments.get(1).getStartSeconds(), 0.001);
        assertEquals(1800.0, segments.get(2).getStartSeconds(), 0.001);
    }

    @Test
    public void testParseSegments_emptyAdsArray() {
        String json = "{\"ads\":[]}";
        List<AdSegment> segments = parseSegments(json);
        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_noAdsField() {
        String json = "{\"segments\":[]}";
        List<AdSegment> segments = parseSegments(json);
        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_nullJson() {
        List<AdSegment> segments = parseSegments(null);
        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_emptyJson() {
        List<AdSegment> segments = parseSegments("");
        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_invalidJson() {
        String json = "not valid json";
        List<AdSegment> segments = parseSegments(json);
        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_missingFields_usesDefaults() {
        String json = "{\"ads\":[{\"startSeconds\":10,\"endSeconds\":40}]}";
        List<AdSegment> segments = parseSegments(json);

        assertEquals(1, segments.size());
        assertEquals("", segments.get(0).getReason());
        assertEquals(0.0, segments.get(0).getConfidence(), 0.001);
    }

    @Test
    public void testParseSegments_invalidSegment_endBeforeStart() {
        String json = "{\"ads\":[{\"startSeconds\":50,\"endSeconds\":30,\"reason\":\"invalid\",\"confidence\":0.5}]}";
        List<AdSegment> segments = parseSegments(json);
        // Should skip segments where end <= start
        assertTrue(segments.isEmpty());
    }

    @Test
    public void testParseSegments_validAndInvalidMixed() {
        String json = "{\"ads\":["
                + "{\"startSeconds\":10,\"endSeconds\":40,\"reason\":\"valid\",\"confidence\":0.9},"
                + "{\"startSeconds\":50,\"endSeconds\":30,\"reason\":\"invalid\",\"confidence\":0.5},"
                + "{\"startSeconds\":100,\"endSeconds\":130,\"reason\":\"valid2\",\"confidence\":0.8}"
                + "]}";
        List<AdSegment> segments = parseSegments(json);

        assertEquals(2, segments.size());
        assertEquals("valid", segments.get(0).getReason());
        assertEquals("valid2", segments.get(1).getReason());
    }

    @Test
    public void testParseSegments_floatValues() {
        String json = "{\"ads\":[{\"startSeconds\":10.5,\"endSeconds\":40.75,"
                + "\"reason\":\"test\",\"confidence\":0.987}]}";
        List<AdSegment> segments = parseSegments(json);

        assertEquals(1, segments.size());
        assertEquals(10.5, segments.get(0).getStartSeconds(), 0.001);
        assertEquals(40.75, segments.get(0).getEndSeconds(), 0.001);
        assertEquals(
                0.987, segments.get(0).getConfidence(), 0.001);
    }

    // ==================== Segment Merging Tests ====================

    @Test
    public void testMergeSegments_noOverlap() {
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "first", 0.9),
                new AdSegment(100, 130, "second", 0.8)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(2, merged.size());
    }

    @Test
    public void testMergeSegments_overlapping() {
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "first", 0.9),
                new AdSegment(25, 55, "second", 0.8)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(1, merged.size());
        assertEquals(0.0, merged.get(0).getStartSeconds(), 0.001);
        assertEquals(55.0, merged.get(0).getEndSeconds(), 0.001);
    }

    @Test
    public void testMergeSegments_adjacent() {
        // Adjacent segments (within 0.5 seconds) should be merged
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "first", 0.9),
                new AdSegment(30.3, 60, "second", 0.8)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(1, merged.size());
        assertEquals(0.0, merged.get(0).getStartSeconds(), 0.001);
        assertEquals(60.0, merged.get(0).getEndSeconds(), 0.001);
    }

    @Test
    public void testMergeSegments_notQuiteAdjacent() {
        // Segments more than 0.5 seconds apart should not be merged
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "first", 0.9),
                new AdSegment(31, 60, "second", 0.8)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(2, merged.size());
    }

    @Test
    public void testMergeSegments_multipleOverlaps() {
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "a", 0.9),
                new AdSegment(25, 55, "b", 0.8),
                new AdSegment(50, 80, "c", 0.85),
                new AdSegment(200, 230, "d", 0.7)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(2, merged.size());
        assertEquals(0.0, merged.get(0).getStartSeconds(), 0.001);
        assertEquals(80.0, merged.get(0).getEndSeconds(), 0.001);
        assertEquals(200.0, merged.get(1).getStartSeconds(), 0.001);
    }

    @Test
    public void testMergeSegments_emptyInput() {
        List<AdSegment> input = Collections.emptyList();
        List<AdSegment> merged = mergeSegments(input);
        assertTrue(merged.isEmpty());
    }

    @Test
    public void testMergeSegments_singleSegment() {
        List<AdSegment> input = Collections.singletonList(
                new AdSegment(10, 40, "only", 0.9)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(1, merged.size());
    }

    @Test
    public void testMergeSegments_unsortedInput() {
        // Input might be unsorted; merging should sort first
        List<AdSegment> input = Arrays.asList(
                new AdSegment(100, 130, "second", 0.8),
                new AdSegment(0, 30, "first", 0.9)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(2, merged.size());
        assertEquals(0.0, merged.get(0).getStartSeconds(), 0.001);
        assertEquals(100.0, merged.get(1).getStartSeconds(), 0.001);
    }

    @Test
    public void testMergeSegments_preservesHigherConfidence() {
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "first", 0.7),
                new AdSegment(25, 55, "second", 0.95)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(1, merged.size());
        assertEquals(0.95, merged.get(0).getConfidence(), 0.001);
    }

    @Test
    public void testMergeSegments_preservesFirstNonEmptyReason() {
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "sponsor read", 0.9),
                new AdSegment(25, 55, "mid-roll", 0.8)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(1, merged.size());
        assertEquals("sponsor read", merged.get(0).getReason());
    }

    @Test
    public void testMergeSegments_emptyReasonUsesNext() {
        List<AdSegment> input = Arrays.asList(
                new AdSegment(0, 30, "", 0.9),
                new AdSegment(25, 55, "ad segment", 0.8)
        );

        List<AdSegment> merged = mergeSegments(input);

        assertEquals(1, merged.size());
        assertEquals("ad segment", merged.get(0).getReason());
    }

    // ==================== Time Parsing Tests ====================

    @Test
    public void testParseSeconds_validFormat() {
        assertEquals(3661.5, parseSeconds("01:01:01.500"), 0.001);
    }

    @Test
    public void testParseSeconds_zeroTime() {
        assertEquals(0.0, parseSeconds("00:00:00.000"), 0.001);
    }

    @Test
    public void testParseSeconds_hoursOnly() {
        assertEquals(3600.0, parseSeconds("01:00:00.000"), 0.001);
    }

    @Test
    public void testParseSeconds_minutesOnly() {
        assertEquals(60.0, parseSeconds("00:01:00.000"), 0.001);
    }

    @Test
    public void testParseSeconds_secondsOnly() {
        assertEquals(30.5, parseSeconds("00:00:30.500"), 0.001);
    }

    @Test
    public void testParseSeconds_largeValue() {
        // 10 hours
        assertEquals(36000.0, parseSeconds("10:00:00.000"), 0.001);
    }

    @Test
    public void testParseSeconds_comma_instead_of_dot() {
        // Some locales use comma as decimal separator
        assertEquals(30.5, parseSeconds("00:00:30,500"), 0.001);
    }

    @Test
    public void testParseSeconds_invalidFormat() {
        assertEquals(0.0, parseSeconds("invalid"), 0.001);
    }

    @Test
    public void testParseSeconds_twoPartsOnly() {
        assertEquals(0.0, parseSeconds("01:30"), 0.001);
    }

    // ==================== Time Formatting Tests ====================

    @Test
    public void testFormatTime_zero() {
        assertEquals("00:00:00.000", formatTime(0));
    }

    @Test
    public void testFormatTime_oneHour() {
        assertEquals("01:00:00.000", formatTime(3600));
    }

    @Test
    public void testFormatTime_oneMinute() {
        assertEquals("00:01:00.000", formatTime(60));
    }

    @Test
    public void testFormatTime_withMillis() {
        assertEquals("00:00:30.500", formatTime(30.5));
    }

    @Test
    public void testFormatTime_complex() {
        // 1h 23m 45.678s
        assertEquals("01:23:45.678", formatTime(5025.678));
    }

    @Test
    public void testFormatTime_largeValue() {
        // 10 hours
        assertEquals("10:00:00.000", formatTime(36000));
    }

    // ==================== VTT Offset Application Tests ====================

    @Test
    public void testApplyOffset_simpleTimestamp() {
        String vtt = "00:00:10.000 --> 00:00:20.000\nHello world";
        String result = applyOffset(vtt, 60.0);

        assertTrue(result.contains("00:01:10.000"));
        assertTrue(result.contains("00:01:20.000"));
    }

    @Test
    public void testApplyOffset_multipleTimestamps() {
        String vtt = "00:00:00.000 --> 00:00:10.000\nFirst\n\n00:00:10.000 --> 00:00:20.000\nSecond";
        String result = applyOffset(vtt, 150.0); // 2.5 minutes

        assertTrue(result.contains("00:02:30.000"));
        assertTrue(result.contains("00:02:40.000"));
        assertTrue(result.contains("00:02:50.000"));
    }

    @Test
    public void testApplyOffset_removesWebvttHeader() {
        String vtt = "WEBVTT\n\n00:00:00.000 --> 00:00:10.000\nText";
        String result = applyOffset(vtt, 0);

        assertFalse(result.toUpperCase().contains("WEBVTT"));
    }

    @Test
    public void testApplyOffset_preservesTextContent() {
        String vtt = "00:00:00.000 --> 00:00:10.000\nHello, this is a test.";
        String result = applyOffset(vtt, 30.0);

        assertTrue(result.contains("Hello, this is a test."));
    }

    @Test
    public void testApplyOffset_zeroOffset() {
        String vtt = "00:00:30.000 --> 00:00:40.000\nText";
        String result = applyOffset(vtt, 0);

        assertTrue(result.contains("00:00:30.000"));
        assertTrue(result.contains("00:00:40.000"));
    }

    // ==================== Transcript Splitting Tests ====================

    @Test
    public void testSplitTranscript_shortTranscript() {
        String transcript = "Short transcript";
        List<String> chunks = splitTranscriptIfNeeded(transcript);

        assertEquals(1, chunks.size());
        assertEquals(transcript, chunks.get(0));
    }

    @Test
    public void testSplitTranscript_exactlyMaxSize() {
        int maxSize = 100000;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxSize; i++) {
            sb.append('a');
        }
        String transcript = sb.toString();
        List<String> chunks = splitTranscriptIfNeeded(transcript);

        assertEquals(1, chunks.size());
    }

    @Test
    public void testSplitTranscript_largeTranscript() {
        int maxSize = 100000;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxSize * 2 + 1000; i++) {
            sb.append('a');
        }
        String transcript = sb.toString();
        List<String> chunks = splitTranscriptIfNeeded(transcript);

        assertTrue(chunks.size() > 1);
    }

    @Test
    public void testSplitTranscript_preservesAllContent() {
        int maxSize = 100000;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxSize * 2; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        String transcript = sb.toString();
        List<String> chunks = splitTranscriptIfNeeded(transcript);

        StringBuilder combined = new StringBuilder();
        for (String chunk : chunks) {
            combined.append(chunk);
        }
        assertEquals(transcript, combined.toString());
    }

    // ==================== Error Detection Tests ====================

    @Test
    public void testIsUnauthorized_401Message() {
        assertTrue(isUnauthorized("401 Unauthorized"));
    }

    @Test
    public void testIsUnauthorized_unauthorizedMessage() {
        assertTrue(isUnauthorized("Unauthorized access"));
    }

    @Test
    public void testIsUnauthorized_otherError() {
        assertFalse(isUnauthorized("Connection timeout"));
    }

    @Test
    public void testIsMemoryError_notEnoughMemory() {
        assertTrue(isMemoryError("Not enough memory to load model"));
    }

    @Test
    public void testIsMemoryError_insufficientMemory() {
        assertTrue(isMemoryError("Insufficient memory available"));
    }

    @Test
    public void testIsMemoryError_notEnoughSystemRam() {
        assertTrue(isMemoryError("Not enough system RAM"));
    }

    @Test
    public void testIsMemoryError_otherError() {
        assertFalse(isMemoryError("Connection timeout"));
    }

    @Test
    public void testMergeSegments_doesNotMutateInputOrder() {
        List<AdSegment> input = Arrays.asList(
                new AdSegment(100, 130, "second", 0.8),
                new AdSegment(0, 30, "first", 0.9)
        );

        mergeSegments(input);

        assertEquals(100.0, input.get(0).getStartSeconds(), 0.001);
        assertEquals(0.0, input.get(1).getStartSeconds(), 0.001);
    }

    @Test
    public void testTranscriptionThreadCountPolicy_minimumOneThreadWhenMemoryIsTight() {
        int threadCount = TranscriptionThreadCountPolicy.chooseThreadCount(8, 256L * 1024L * 1024L,
                128L * 1024L * 1024L);

        assertEquals(1, threadCount);
    }

    @Test
    public void testTranscriptionThreadCountPolicy_limitsByMemoryAndCpu() {
        int threadCount = TranscriptionThreadCountPolicy.chooseThreadCount(8, 1024L * 1024L * 1024L,
                256L * 1024L * 1024L);

        assertEquals(8, threadCount);
    }

    @Test
    public void testTranscriptionThreadCountPolicy_limitsByConfiguredMaximum() {
        int threadCount = TranscriptionThreadCountPolicy.chooseThreadCount(8, 1024L * 1024L * 1024L,
                256L * 1024L * 1024L, 5);

        assertEquals(5, threadCount);
    }

    // ==================== Helper Methods (delegate to production utilities) ====================

    private String sanitizeJson(String raw) {
        return AdSegmentJsonParser.sanitize(raw);
    }

    private List<AdSegment> parseSegments(String json) {
        return AdSegmentJsonParser.parse(json);
    }

    private List<AdSegment> mergeSegments(List<AdSegment> input) {
        return AdSegmentMerger.merge(input);
    }

    private double parseSeconds(String timeString) {
        return VttTimestampAdjuster.parseSeconds(timeString);
    }

    private String formatTime(double seconds) {
        return VttTimestampAdjuster.formatTime(seconds);
    }

    private String applyOffset(String vtt, double offsetSeconds) {
        return VttTimestampAdjuster.applyOffset(vtt, offsetSeconds);
    }

    private List<String> splitTranscriptIfNeeded(String transcript) {
        return TranscriptChunker.split(transcript, AdAnalysisConfig.MAX_TRANSCRIPT_CHARS_PER_CHUNK);
    }

    private boolean isUnauthorized(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.US);
        return normalized.contains("unauthorized") || normalized.contains("401");
    }

    private boolean isMemoryError(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.US);
        return normalized.contains("not enough memory") || normalized.contains("insufficient memory")
                || normalized.contains("not enough system ram");
    }

}
