package de.test.antennapod.gpodnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.test.runner.AndroidJUnit4;
import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetDevice;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetTag;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Collections.singletonList;

/**
 * Test class for GpodnetService
 */
@Ignore
@RunWith(AndroidJUnit4.class)
public class GPodnetServiceTest {

    private GpodnetService service;

    private static final String USER = "";
    private static final String PW = "";

    @Before
    protected void setUp() {
        service = new GpodnetService();
    }

    private void authenticate() throws GpodnetServiceException {
        service.authenticate(USER, PW);
    }

    @Test
    public void testUploadSubscription() throws GpodnetServiceException {
        authenticate();
        ArrayList<String> l = new ArrayList<>();
        l.add("http://bitsundso.de/feed");
        service.uploadSubscriptions(USER, "radio", l);
    }

    @Test
    public void testUploadSubscription2() throws GpodnetServiceException {
        authenticate();
        ArrayList<String> l = new ArrayList<>();
        l.add("http://bitsundso.de/feed");
        l.add("http://gamesundso.de/feed");
        service.uploadSubscriptions(USER, "radio", l);
    }

    @Test
    public void testUploadChanges() throws GpodnetServiceException {
        authenticate();
        String[] URLS = {"http://bitsundso.de/feed", "http://gamesundso.de/feed", "http://cre.fm/feed/mp3/", "http://freakshow.fm/feed/m4a/"};
        List<String> subscriptions = Arrays.asList(URLS[0], URLS[1]);
        List<String> removed = singletonList(URLS[0]);
        List<String> added = Arrays.asList(URLS[2], URLS[3]);
        service.uploadSubscriptions(USER, "radio", subscriptions);
        service.uploadChanges(USER, "radio", added, removed);
    }

    @Test
    public void testGetSubscriptionChanges() throws GpodnetServiceException {
        authenticate();
        service.getSubscriptionChanges(USER, "radio", 1362322610L);
    }

    @Test
    public void testGetSubscriptionsOfUser()
            throws GpodnetServiceException {
        authenticate();
        service.getSubscriptionsOfUser(USER);
    }

    @Test
    public void testGetSubscriptionsOfDevice()
            throws GpodnetServiceException {
        authenticate();
        service.getSubscriptionsOfDevice(USER, "radio");
    }

    @Test
    public void testConfigureDevices() throws GpodnetServiceException {
        authenticate();
        service.configureDevice(USER, "foo", "This is an updated caption",
                GpodnetDevice.DeviceType.LAPTOP);
    }

    @Test
    public void testGetDevices() throws GpodnetServiceException {
        authenticate();
        service.getDevices(USER);
    }

    @Test
    public void testGetSuggestions() throws GpodnetServiceException {
        authenticate();
        service.getSuggestions(10);
    }

    @Test
    public void testTags() throws GpodnetServiceException {
        service.getTopTags(20);
    }

    @Test
    public void testPodcastForTags() throws GpodnetServiceException {
        List<GpodnetTag> tags = service.getTopTags(20);
        service.getPodcastsForTag(tags.get(1),
                10);
    }

    @Test
    public void testSearch() throws GpodnetServiceException {
        service.searchPodcasts("linux", 64);
    }

    @Test
    public void testToplist() throws GpodnetServiceException {
        service.getPodcastToplist(10);
    }
}
