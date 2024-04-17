package de.danoeh.antennapod.model.feed;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Transcript {

    private final TreeMap<Long, TranscriptSegment> segmentsMap = new TreeMap<>();

    public void addSegment(TranscriptSegment segment) {
        segmentsMap.put(segment.getStartTime(), segment);
    }

    public TreeMap<Long, TranscriptSegment> getSegmentsMap() {
        return segmentsMap;
    }
    
    public TranscriptSegment getSegmentAtTime(long time) {
        if (segmentsMap.floorEntry(time) == null) {
            return null;
        }
        return segmentsMap.floorEntry(time).getValue();
    }
    // other methods and fields...

    private Set<String> speakers;

    public Set<String> getSpeakers() {
       return speakers;
    }

    public void setSpeakers(Set<String> speakers) {
        this.speakers = speakers;
    }

    public int getSegmentCount() {
        return segmentsMap.size();
    }

    public Map.Entry<Long, TranscriptSegment> getEntryAfterTime(long time) {
        return segmentsMap.ceilingEntry(time);
    }
}
