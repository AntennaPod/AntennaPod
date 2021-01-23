package de.danoeh.antennapod.core.ssl;

import de.danoeh.antennapod.core.util.Flavors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

/**
 * SSLSocketFactory that does not use TLS 1.0
 * This fixes issues with old Android versions that abort if the server does not know TLS 1.0
 */
public class NoV1SslSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory;

    public NoV1SslSocketFactory(TrustManager trustManager) {
        try {
            SSLContext sslContext;

            if (Flavors.FLAVOR == Flavors.FREE) {
                // Free flavor (bundles modern conscrypt): support for TLSv1.3 is guaranteed.
                sslContext = SSLContext.getInstance("TLSv1.3");
            } else {
                // Play flavor (security provider can vary): only TLSv1.2 is guaranteed.
                sslContext = SSLContext.getInstance("TLSv1.2");
            }

            sslContext.init(null, new TrustManager[] {trustManager}, null);
            factory = sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    public Socket createSocket() throws IOException {
        SSLSocket result = (SSLSocket) factory.createSocket();
        configureSocket(result);
        return result;
    }

    public Socket createSocket(String var1, int var2) throws IOException {
        SSLSocket result = (SSLSocket) factory.createSocket(var1, var2);
        configureSocket(result);
        return result;
    }

    public Socket createSocket(Socket var1, String var2, int var3, boolean var4) throws IOException {
        SSLSocket result = (SSLSocket) factory.createSocket(var1, var2, var3, var4);
        configureSocket(result);
        return result;
    }

    public Socket createSocket(InetAddress var1, int var2) throws IOException {
        SSLSocket result = (SSLSocket) factory.createSocket(var1, var2);
        configureSocket(result);
        return result;
    }

    public Socket createSocket(String var1, int var2, InetAddress var3, int var4) throws IOException {
        SSLSocket result = (SSLSocket) factory.createSocket(var1, var2, var3, var4);
        configureSocket(result);
        return result;
    }

    public Socket createSocket(InetAddress var1, int var2, InetAddress var3, int var4) throws IOException {
        SSLSocket result = (SSLSocket) factory.createSocket(var1, var2, var3, var4);
        configureSocket(result);
        return result;
    }

    private void configureSocket(SSLSocket s) {
        if (Flavors.FLAVOR == Flavors.FREE) {
            // Free flavor (bundles modern conscrypt): TLSv1.3 and modern cipher suites are
            // guaranteed. Protocols older than TLSv1.2 are now deprecated and can be disabled.
            s.setEnabledProtocols(new String[] { "TLSv1.3", "TLSv1.2" });
        } else {
            // Play flavor (security provider can vary): only TLSv1.2 is guaranteed, supported
            // cipher suites may vary. Old protocols might be necessary to keep things working.

            // TLS 1.0 is enabled by default on some old systems, which causes connection errors.
            // This disables that.
            s.setEnabledProtocols(new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" });
        }
    }
}