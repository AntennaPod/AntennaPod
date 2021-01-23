package de.danoeh.antennapod.core.ssl;

import android.util.Log;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * SSL trust manager that allows old Android systems to use modern certificates.
 */
public class BackportTrustManager {
    private static final String TAG = "BackportTrustManager";

    private static X509TrustManager getSystemTrustManager(KeyStore keystore) {
        TrustManagerFactory factory;
        try {
            factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(keystore);
            for (TrustManager manager : factory.getTrustManagers()) {
                if (manager instanceof X509TrustManager) {
                    return (X509TrustManager) manager;
                }
            }
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Unexpected default trust managers");
    }

    public static X509TrustManager create() {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null); // Clear
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keystore.setCertificateEntry("BACKPORT_COMODO_ROOT_CA", cf.generateCertificate(
                    new ByteArrayInputStream(BackportCaCerts.COMODO.getBytes(Charset.forName("UTF-8")))));
            keystore.setCertificateEntry("SECTIGO_USER_TRUST_CA", cf.generateCertificate(
                    new ByteArrayInputStream(BackportCaCerts.SECTIGO_USER_TRUST.getBytes(Charset.forName("UTF-8")))));
            keystore.setCertificateEntry("LETSENCRYPT_ISRG_CA", cf.generateCertificate(
                    new ByteArrayInputStream(BackportCaCerts.LETSENCRYPT_ISRG.getBytes(Charset.forName("UTF-8")))));

            List<X509TrustManager> managers = new ArrayList<>();
            managers.add(getSystemTrustManager(keystore));
            managers.add(getSystemTrustManager(null));
            return new CompositeX509TrustManager(managers);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return null;
        }
    }
}
