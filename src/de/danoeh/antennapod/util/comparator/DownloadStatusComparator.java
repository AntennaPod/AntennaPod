package de.danoeh.antennapod.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.service.download.*;

/** Compares the completion date of two Downloadstatus objects. */
public class DownloadStatusComparator implements Comparator<DownloadStatus> {

	@Override
	public int compare(DownloadStatus lhs, DownloadStatus rhs) {
		return -lhs.getCompletionDate().compareTo(rhs.getCompletionDate());

	}

}
