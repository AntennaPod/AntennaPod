package de.danoeh.antennapod.core.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.model.download.DownloadStatus;

/** Compares the completion date of two Downloadstatus objects. */
public class DownloadStatusComparator implements Comparator<DownloadStatus> {

	@Override
	public int compare(DownloadStatus lhs, DownloadStatus rhs) {
		return rhs.getCompletionDate().compareTo(lhs.getCompletionDate());
	}

}
