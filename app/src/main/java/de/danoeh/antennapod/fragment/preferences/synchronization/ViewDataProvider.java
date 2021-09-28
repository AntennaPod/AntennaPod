package de.danoeh.antennapod.fragment.preferences.synchronization;

import android.content.Context;

import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;

import java.util.EnumSet;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.sync.SynchronizationCredentials;
import de.danoeh.antennapod.core.sync.SynchronizationProviderViewData;

public class ViewDataProvider {

    public static String getUsernameFromSelectedSyncProvider(Context context, String currentSyncProviderKey) {
        SynchronizationProviderViewData currentSyncProvider = SynchronizationProviderViewData
                .valueOf(currentSyncProviderKey);
        switch (currentSyncProvider) {
            case GPODDER_NET:
                return SynchronizationCredentials.getUsername();
            case NEXTCLOUD_GPODDER:
                try {
                    return SingleAccountHelper.getCurrentSingleSignOnAccount(context).name;
                } catch (NextcloudFilesAppAccountNotFoundException e) {
                    e.printStackTrace();
                    return "";
                } catch (NoCurrentAccountSelectedException e) {
                    e.printStackTrace();
                    return "";
                }
            default:
                return "";
        }
    }

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
