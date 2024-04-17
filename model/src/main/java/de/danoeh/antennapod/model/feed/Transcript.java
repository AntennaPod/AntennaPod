package de.danoeh.antennapod.model.feed;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Transcript {

    private final TreeMap<Long, TranscriptSegment> segmentsMap = new TreeMap<>();
    private Set<String> speakers;
    public Hashtable<Long, Integer> positions;
    Object[] objs;

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

    public Integer getIndex(Map.Entry<Long, TranscriptSegment> entry) {
        buildSequentialAccess();
        return positions.get(entry.getKey());
    }

    public Long getTimeCode(int index) {
        buildSequentialAccess();
        return (Long) ((TreeMap.Entry) objs[index]).getKey();
    }

    public TranscriptSegment getSegmentAt(int index) {
        buildSequentialAccess();
        return (TranscriptSegment) ((TreeMap.Entry) objs[index]).getValue();
    }

    private void buildSequentialAccess() {
        if (positions != null) {
            return;
        }

        objs = segmentsMap.entrySet().toArray();
        positions = new Hashtable<Long, Integer>();
        for ( int i = 0; i < objs.length; i++) {
            Map.Entry<Long, TranscriptSegment> seg;
            seg = (Map.Entry<Long, TranscriptSegment>) objs[i];
            positions.put((Long) seg.getKey(), i);
        }
    }

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
