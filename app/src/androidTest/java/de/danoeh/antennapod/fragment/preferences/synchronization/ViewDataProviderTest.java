package de.danoeh.antennapod.fragment.preferences.synchronization;

import junit.framework.TestCase;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.sync.SynchronizationProviderViewData;

public class ViewDataProviderTest extends TestCase {

    public void testGetSynchronizationProviderHeaderSummary() {
        assertEquals(R.string.synchronization_summary_nextcloud,
                ViewDataProvider.getSynchronizationProviderHeaderSummary(
                        SynchronizationProviderViewData.NEXTCLOUD_GPODDER.getIdentifier()));

        assertEquals(R.string.synchronization_summary_unchoosen,
                ViewDataProvider.getSynchronizationProviderHeaderSummary(
                        "unset"));
    }

    public void testGetSynchronizationProviderIcon() {
        assertEquals(R.drawable.nextcloud_logo, ViewDataProvider.getSynchronizationProviderIcon(
                SynchronizationProviderViewData.NEXTCLOUD_GPODDER.getIdentifier()));

        assertEquals(R.drawable.ic_cloud, ViewDataProvider.getSynchronizationProviderIcon(null));
    }
}
