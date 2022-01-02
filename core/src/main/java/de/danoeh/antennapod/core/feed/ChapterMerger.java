package de.danoeh.antennapod.core.feed;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.Chapter;

import java.util.List;

public class ChapterMerger {
    private static final String TAG = "ChapterMerger";

    private ChapterMerger() {

    }

    /**
     * This method might modify the input data.
     */
    @Nullable
    public static List<Chapter> merge(@Nullable List<Chapter> chapters1, @Nullable List<Chapter> chapters2) {
        Log.d(TAG, "Merging chapters");
        if (chapters1 == null) {
            return chapters2;
        } else if (chapters2 == null) {
            return chapters1;
        } else if (chapters2.size() > chapters1.size()) {
            return chapters2;
        } else if (chapters2.size() < chapters1.size()) {
            return chapters1;
        } else {
            // Merge chapter lists of same length. Store in chapters2 array.
            // In case the lists can not be merged, return chapters1 array.
            for (int i = 0; i < chapters2.size(); i++) {
                Chapter chapterTarget = chapters2.get(i);
                Chapter chapterOther = chapters1.get(i);

                if (Math.abs(chapterTarget.getStart() - chapterOther.getStart()) > 1000) {
                    Log.e(TAG, "Chapter lists are too different. Cancelling merge.");
                    return chapters1;
                }

                if (TextUtils.isEmpty(chapterTarget.getImageUrl())) {
                    chapterTarget.setImageUrl(chapterOther.getImageUrl());
                }
                if (TextUtils.isEmpty(chapterTarget.getLink())) {
                    chapterTarget.setLink(chapterOther.getLink());
                }
                if (TextUtils.isEmpty(chapterTarget.getTitle())) {
                    chapterTarget.setTitle(chapterOther.getTitle());
                }
            }
            return chapters2;
        }
    }
}
