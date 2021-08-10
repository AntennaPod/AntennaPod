package de.danoeh.antennapod.fragment.preferences.synchronization;

import android.content.Context;

import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountNotFoundException;
import com.nextcloud.android.sso.exceptions.NoCurrentAccountSelectedException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;

import java.util.ArrayList;
import java.util.EnumSet;

import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.SynchronizationProviderViewData;

public class ViewDataProvider {

    public static String getUsernameFromSelectedSyncProvider(Context context, String currentSyncProviderKey) {
        switch (currentSyncProviderKey) {
            case SyncService.SYNC_PROVIDER_CHOICE_GPODDER_NET:
                return GpodnetPreferences.getUsername();
            case SyncService.SYNC_PROVIDER_CHOICE_NEXTCLOUD:
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
        return getSelectedSynchronizationProviderViewData(providerName).getSummaryResource();
    }

    public static int getSynchronizationProviderIcon(String providerName) {
        return getSelectedSynchronizationProviderViewData(providerName).getIconResource();
    }

    private static SynchronizationProviderViewData getSelectedSynchronizationProviderViewData(String provider) {
        EnumSet<SynchronizationProviderViewData> availableSynchronizationProviders =
                EnumSet.allOf(SynchronizationProviderViewData.class);
        for (SynchronizationProviderViewData synchronizationProvider :
                availableSynchronizationProviders) {
            if (synchronizationProvider.getName().equals(provider)) {
                return synchronizationProvider;
            }
        }
        return null;
    }

    public static ArrayList<Integer> getAllSynchronizationProviderDescriptionResources() {
        EnumSet<SynchronizationProviderViewData> allProviderViewData =
                getAllImplementedSynchronizationProviderViewData();
        ArrayList<Integer> descriptionResources = new ArrayList<>();
        for (SynchronizationProviderViewData synchronizationProviderViewData : allProviderViewData) {
            descriptionResources.add(synchronizationProviderViewData.getSummaryResource());
        }

        return descriptionResources;
    }

    public static ArrayList<Integer> getAllSynchronizationProviderIconResources() {
        EnumSet<SynchronizationProviderViewData> allProviderViewData =
                getAllImplementedSynchronizationProviderViewData();
        ArrayList<Integer> iconResources = new ArrayList<>();
        for (SynchronizationProviderViewData synchronizationProviderViewData : allProviderViewData) {
            iconResources.add(synchronizationProviderViewData.getIconResource());
        }

        return iconResources;
    }


    private static EnumSet<SynchronizationProviderViewData> getAllImplementedSynchronizationProviderViewData() {
        EnumSet<SynchronizationProviderViewData> allProviderViewData =
                EnumSet.allOf(SynchronizationProviderViewData.class);
        allProviderViewData.remove(SynchronizationProviderViewData.NONE);
        return allProviderViewData;
    }
}
