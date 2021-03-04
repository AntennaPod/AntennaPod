package de.danoeh.antennapod.net.ssl;

import android.content.Context;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

public class SslProviderInstaller {
    public static void install(Context context) {
        try {
            ProviderInstaller.installIfNeeded(context);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
            GoogleApiAvailability.getInstance().showErrorNotification(context, e.getConnectionStatusCode());
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }
}
