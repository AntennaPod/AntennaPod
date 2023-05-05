package de.danoeh.antennapod.core.util.comparator;

import java.util.Comparator;

import de.danoeh.antennapod.model.download.DownloadResult;

/** Compares the completion date of two DownloadResult objects. */
public class DownloadResultComparator implements Comparator<DownloadResult> {

    @Override
    public int compare(DownloadResult lhs, DownloadResult rhs) {
        return rhs.getCompletionDate().compareTo(lhs.getCompletionDate());
    }
}
