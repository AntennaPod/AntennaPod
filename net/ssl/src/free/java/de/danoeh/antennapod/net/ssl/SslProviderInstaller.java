package de.danoeh.antennapod.net.ssl;

import android.content.Context;
import org.conscrypt.Conscrypt;

import java.security.Security;

public class SslProviderInstaller {
    public static void install(Context context) {
        // Insert bundled conscrypt as highest security provider (overrides OS version).
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
    }
}
