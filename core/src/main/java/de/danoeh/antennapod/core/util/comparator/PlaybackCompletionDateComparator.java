package de.danoeh.antennapod.core.util.comparator;

import android.support.annotation.NonNull;

import java.util.Comparator;

import de.danoeh.antennapod.core.feed.FeedItem;

public class PlaybackCompletionDateComparator implements Comparator<FeedItem> {
	
	public int compare(@NonNull FeedItem lhs, @NonNull FeedItem rhs) {
		if (lhs.getMedia() != null
				&& lhs.getMedia().getPlaybackCompletionDate() != null
				&& rhs.getMedia() != null
				&& rhs.getMedia().getPlaybackCompletionDate() != null) {
			return rhs.getMedia().getPlaybackCompletionDate()
					.compareTo(lhs.getMedia().getPlaybackCompletionDate());
		}
		return 0;
	}
}
