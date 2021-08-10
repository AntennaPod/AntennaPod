package de.danoeh.antennapod.fragment.preferences.synchronization;

import androidx.test.platform.app.InstrumentationRegistry;

import junit.framework.TestCase;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.sync.SyncService;

public class ViewDataProviderTest extends TestCase {

    public void testGetSynchronizationProviderHeaderSummary() {
        assertEquals(R.string.preference_synchronization_summary_nextcloud, SynchronizationProviderViewData.getSynchronizationProviderHeaderSummary(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                SyncService.SYNC_PROVIDER_CHOICE_NEXTCLOUD));

        assertEquals(R.string.preference_synchronization_summary_unchoosen, SynchronizationProviderViewData.getSynchronizationProviderHeaderSummary(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                SynchronizationPreferencesFragment.SYNC_PROVIDER_UNSET));
    }

    public void testGetSynchronizationProviderIcon() {
        assertEquals(R.drawable.nextcloud_logo, SynchronizationProviderViewData.getSynchronizationProviderIcon(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                SyncService.SYNC_PROVIDER_CHOICE_NEXTCLOUD));

        assertEquals(R.drawable.ic_cloud, SynchronizationProviderViewData.getSynchronizationProviderIcon(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                SynchronizationPreferencesFragment.SYNC_PROVIDER_UNSET));
    }
}