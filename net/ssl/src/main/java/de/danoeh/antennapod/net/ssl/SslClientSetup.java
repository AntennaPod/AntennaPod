package de.danoeh.antennapod.net.ssl;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

import javax.net.ssl.X509TrustManager;
import java.util.Arrays;

public class SslClientSetup {
    public static void installCertificates(OkHttpClient.Builder builder) {
        X509TrustManager trustManager = BackportTrustManager.create();
        builder.sslSocketFactory(new AntennaPodSslSocketFactory(trustManager), trustManager);
        builder.connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT));
    }
}
