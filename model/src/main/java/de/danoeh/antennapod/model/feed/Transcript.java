package de.danoeh.antennapod.model.feed;

import java.util.ArrayList;
import java.util.Set;

public class Transcript {
    private Set<String> speakers;
    private final ArrayList<TranscriptSegment> segments = new ArrayList<>();

    public void addSegment(TranscriptSegment segment) {
        if ((!segments.isEmpty() && segments.get(segments.size() - 1).getStartTime() >= segment.getStartTime())) {
            throw new IllegalArgumentException("Segments must be added in sorted order");
        }
        segments.add(segment);
    }

    public int findSegmentIndexBefore(long time) {
        int a = 0;
        int b = segments.size() - 1;
        while (a < b) {
            int pivot = (a + b + 1) / 2;
            if (segments.get(pivot).getStartTime() > time) {
                b = pivot - 1;
            } else {
                a = pivot;
            }
        }
        return a;
    }

    public TranscriptSegment getSegmentAt(int index) {
        return segments.get(index);
    }

    public TranscriptSegment getSegmentAtTime(long time) {
        return getSegmentAt(findSegmentIndexBefore(time));
    }

    public Set<String> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(Set<String> speakers) {
        this.speakers = speakers;
    }

    public int getSegmentCount() {
        return segments.size();
    }
}
