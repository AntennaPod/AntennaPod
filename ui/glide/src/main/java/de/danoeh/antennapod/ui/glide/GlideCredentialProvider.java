package de.danoeh.antennapod.ui.glide;

public class GlideCredentialProvider {

    public interface CredentialProvider {
        String getCredentials(String url);
    }

    private static volatile CredentialProvider credentialProvider;

    public static void setCredentialProvider(CredentialProvider provider) {
        credentialProvider = provider;
    }

    static CredentialProvider getCredentialProvider() {
        return credentialProvider;
    }
}
