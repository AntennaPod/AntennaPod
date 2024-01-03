package de.danoeh.antennapod.model.feed;

import java.util.Map;
import java.util.TreeMap;

public class Transcript {

    private final TreeMap<Long, TranscriptSegment> segmentsMap = new TreeMap<>();

    public void addSegment(TranscriptSegment segment) {
        segmentsMap.put(segment.getStartTime(), segment);
    }

    public void updateSegment(Long timeOld, Long timeNew) {
        Map.Entry<Long, TranscriptSegment> entry1 = segmentsMap.floorEntry(timeOld);
        if (entry1 != null) {
            segmentsMap.remove(entry1.getKey());
            segmentsMap.put(timeNew, entry1.getValue());
        }
    }

    public TranscriptSegment getSegmentAtTime(long time) {
        if (segmentsMap.floorEntry(time) == null) {
            return null;
        }
        return segmentsMap.floorEntry(time).getValue();
    }

    public int getSegmentCount() {
        return segmentsMap.size();
    }

    public Map.Entry<Long, TranscriptSegment> getEntryAfterTime(long time) {
        return segmentsMap.ceilingEntry(time);
    }

    public TranscriptSegment remove(Map.Entry<Long, TranscriptSegment> entry) {
        if (entry == null) {
            return null;
        }
        return segmentsMap.remove(entry.getKey());
    }
}
