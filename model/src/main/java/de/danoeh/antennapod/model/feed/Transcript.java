package de.danoeh.antennapod.model.feed;

import java.util.Map;
import java.util.TreeMap;

public class Transcript {

    private final TreeMap<Long, TranscriptSegment> segmentsMap = new TreeMap<>();

    public void addSegment(TranscriptSegment segment) {
        segmentsMap.put(segment.getStartTime(), segment);
    }

    public void replace(Long k1, Long k2) {
        Map.Entry<Long, TranscriptSegment> entry1 = (Map.Entry<Long, TranscriptSegment>) segmentsMap.floorEntry(k1);
        if (entry1 != null) {
            segmentsMap.remove(entry1.getKey());
            segmentsMap.put(k2, entry1.getValue());
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

    public Map.Entry<Long, TranscriptSegment> getSegmentAfterTime(long time) {
        if (segmentsMap.ceilingEntry(time) == null) {
            return null;
        }
        return segmentsMap.ceilingEntry(time);
    }

    public TranscriptSegment remove(TranscriptSegment seg) {
        if (seg == null) {
            return null;
        }
        return segmentsMap.remove(seg.getStartTime());
    }
}
