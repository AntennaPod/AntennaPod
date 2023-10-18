package de.danoeh.antennapod.model.feed;

import java.util.HashMap;
import java.util.TreeMap;

public class Transcript {

    private TreeMap<Long, TranscriptSegment> segmentsMap = new TreeMap<>();
    private HashMap<Long, TranscriptSegment> randomAccessMap = new HashMap<>();

    public void addSegment(TranscriptSegment segment) {
        segmentsMap.put(segment.getStartTime(), segment);
        randomAccessMap.put(segment.getStartTime(), segment);
    }

    public TreeMap<Long, TranscriptSegment> getSegmentsMap() {
        return segmentsMap;
    }

    public TranscriptSegment getSegmentAtTime(long time) {
        return segmentsMap.floorEntry(time).getValue();
    }

    public TranscriptSegment getSegmentAtRandomTime(long time) {
        return randomAccessMap.get(time);
    }
}
