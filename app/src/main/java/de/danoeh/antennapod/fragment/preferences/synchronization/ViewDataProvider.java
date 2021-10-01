package de.danoeh.antennapod.fragment.preferences.synchronization;

import java.util.EnumSet;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.sync.SynchronizationProviderViewData;

public class ViewDataProvider {

    public static int getSynchronizationProviderHeaderSummary(String providerName) {
        SynchronizationProviderViewData selectedSynchronizationProviderViewData =
                getSelectedSynchronizationProviderViewData(providerName);
        if (selectedSynchronizationProviderViewData == null) {
            return R.string.synchronization_summary_unchoosen;
        }
        return selectedSynchronizationProviderViewData.getSummaryResource();
    }

    public static int getSynchronizationProviderIcon(String providerName) {
        SynchronizationProviderViewData selectedSynchronizationProviderViewData =
                getSelectedSynchronizationProviderViewData(providerName);
        if (selectedSynchronizationProviderViewData == null) {
            return R.drawable.ic_cloud;
        }
        return selectedSynchronizationProviderViewData.getIconResource();
    }

    private static SynchronizationProviderViewData getSelectedSynchronizationProviderViewData(String provider) {
        EnumSet<SynchronizationProviderViewData> availableSynchronizationProviders =
                EnumSet.allOf(SynchronizationProviderViewData.class);
        for (SynchronizationProviderViewData synchronizationProvider :
                availableSynchronizationProviders) {
            if (synchronizationProvider.getIdentifier().equals(provider)) {
                return synchronizationProvider;
            }
        }
        return null;
    }
}
