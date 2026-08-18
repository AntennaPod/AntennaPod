package de.danoeh.antennapod.net.common;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class BasicAuthorizationInterceptorTest {

    @After
    public void tearDown() {
        BasicAuthorizationInterceptor.setCredentialProvider(null);
    }

    @Test
    public void testCredentialProviderCanBeSet() {
        BasicAuthorizationInterceptor.CredentialProvider provider = url -> "user:pass";
        BasicAuthorizationInterceptor.setCredentialProvider(provider);
        assertEquals("user:pass", provider.getCredentials("http://example.com/image.png"));
    }

    @Test
    public void testCredentialProviderReturnsNullForUnknownHost() {
        BasicAuthorizationInterceptor.CredentialProvider provider = url -> {
            if (url.startsWith("http://myserver.com")) {
                return "user:pass";
            }
            return null;
        };
        BasicAuthorizationInterceptor.setCredentialProvider(provider);
        assertNull(provider.getCredentials("http://other.com/image.png"));
        assertEquals("user:pass", provider.getCredentials("http://myserver.com/image.png"));
    }

    @Test
    public void testCredentialProviderCanBeCleared() {
        BasicAuthorizationInterceptor.setCredentialProvider(url -> "user:pass");
        BasicAuthorizationInterceptor.setCredentialProvider(null);
    }
}
