package de.danoeh.antennapod.net.ssl;

import android.os.Build;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

import javax.net.ssl.X509TrustManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SslClientSetup {
    public static void installCertificates(OkHttpClient.Builder builder) {
        X509TrustManager trustManager = BackportTrustManager.create();
        builder.sslSocketFactory(new AntennaPodSslSocketFactory(trustManager), trustManager);

        ConnectionSpec tlsSpec = ConnectionSpec.MODERN_TLS;
        if (BuildConfig.FLAVOR.equals("play") && Build.VERSION.SDK_INT < 21) {
            // workaround for Android 4.x for certain web sites.
            // see: https://github.com/square/okhttp/issues/4053#issuecomment-402579554
            List<CipherSuite> cipherSuites = new ArrayList<>(ConnectionSpec.MODERN_TLS.cipherSuites());
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA);
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA);
            tlsSpec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .cipherSuites(cipherSuites.toArray(new CipherSuite[0]))
                    .build();
        }
        builder.connectionSpecs(Arrays.asList(tlsSpec, ConnectionSpec.CLEARTEXT));
    }
}
