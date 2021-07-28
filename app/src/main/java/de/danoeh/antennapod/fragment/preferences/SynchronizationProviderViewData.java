package de.danoeh.antennapod.fragment.preferences;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

import de.danoeh.antennapod.R;

public class SynchronizationProviderViewData {

    public static int getSynchronizationProviderHeaderSummary(Context context, String provider) {
        return getSynchronizationProviderPropertyId(context, provider, R.array.sync_provider_summary, R.string.class);
    }

    public static int getSynchronizationProviderIcon(Context context, String provider) {
        return getSynchronizationProviderPropertyId(context, provider, R.array.sync_provider_icon, R.drawable.class);
    }

    private static int getSynchronizationProviderPropertyId(Context context, String provider, int propertyArrayId, Class type) {
        final String[] availableProviders = context.getResources().getStringArray(R.array.sync_provider_keys);
        final LinkedHashMap<String, String> syncProvidersValues = getSyncProviderValuesByProperty(context, availableProviders, propertyArrayId);
        String resourceKey = syncProvidersValues.get(provider);
        int stringId = 0;
        try {
            Field field = type.getField(resourceKey);
            stringId = field.getInt(null);
        } catch (Exception e) {
            Log.d("SyncService", "view data for syncprovider missing");
        }
        return stringId;

    }

    private static LinkedHashMap<String, String> getSyncProviderValuesByProperty(Context context, String[] availableProviders, int sync_provider_property) {
        final String[] values = context.getResources().getStringArray(sync_provider_property);
        LinkedHashMap<String,String> mappedProperties = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(availableProviders.length, values.length); ++i) {
            mappedProperties.put(availableProviders[i], values[i]);
        }
        return mappedProperties;
    }
}
