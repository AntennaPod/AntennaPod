package de.danoeh.antennapod.fragment.preferences.synchronization;

import junit.framework.TestCase;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.sync.SyncService;

public class ViewDataProviderTest extends TestCase {

    public void testGetSynchronizationProviderHeaderSummary() {
        assertEquals(R.string.synchronization_summary_nextcloud,
                ViewDataProvider.getSynchronizationProviderHeaderSummary(
                        SyncService.SYNC_PROVIDER_CHOICE_NEXTCLOUD));

        assertEquals(R.string.synchronization_summary_unchoosen,
                ViewDataProvider.getSynchronizationProviderHeaderSummary(
                        SynchronizationPreferencesFragment.SYNC_PROVIDER_UNSET));
    }

    public void testGetSynchronizationProviderIcon() {
        assertEquals(R.drawable.nextcloud_logo, ViewDataProvider.getSynchronizationProviderIcon(
                SyncService.SYNC_PROVIDER_CHOICE_NEXTCLOUD));

        assertEquals(R.drawable.ic_cloud, ViewDataProvider.getSynchronizationProviderIcon(
                SynchronizationPreferencesFragment.SYNC_PROVIDER_UNSET));
    }
}