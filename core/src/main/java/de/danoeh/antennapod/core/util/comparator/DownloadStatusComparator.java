package de.danoeh.antennapod.core.util.comparator;

import android.support.annotation.NonNull;

import java.util.Comparator;

import de.danoeh.antennapod.core.service.download.DownloadStatus;

/** Compares the completion date of two Downloadstatus objects. */
public class DownloadStatusComparator implements Comparator<DownloadStatus> {

	@Override
	public int compare(@NonNull DownloadStatus lhs, @NonNull DownloadStatus rhs) {
		return rhs.getCompletionDate().compareTo(lhs.getCompletionDate());
	}

}
