package de.test.antennapod.gpodnet;

import android.test.AndroidTestCase;

import de.danoeh.antennapod.core.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetDevice;
import de.danoeh.antennapod.core.gpoddernet.model.GpodnetTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test class for GpodnetService
 */
public class GPodnetServiceTest extends AndroidTestCase {

    private GpodnetService service;

    private static final String USER = "";
    private static final String PW = "";
    private static final String RADIO = "radio";
    private static final String BITSUDSO_FEED_URL = "http://bitsundso.de/feed";


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        service = new GpodnetService();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void authenticate() throws GpodnetServiceException {
        service.authenticate(USER, PW);
    }

    public void testUploadSubscription() throws GpodnetServiceException {
        authenticate();
        ArrayList<String> l = new ArrayList<String>();
        l.add(BITSUDSO_FEED_URL);
        service.uploadSubscriptions(USER, RADIO, l);
    }

    public void testUploadSubscription2() throws GpodnetServiceException {
        authenticate();
        ArrayList<String> l = new ArrayList<String>();
        l.add(BITSUDSO_FEED_URL);
        l.add("http://gamesundso.de/feed");
        service.uploadSubscriptions(USER, RADIO, l);
    }

    public void testUploadChanges() throws GpodnetServiceException {
        authenticate();
        String[] URLS = {BITSUDSO_FEED_URL, "http://gamesundso.de/feed", "http://cre.fm/feed/mp3/", "http://freakshow.fm/feed/m4a/"};
        List<String> subscriptions = Arrays.asList(URLS[0], URLS[1]);
        List<String> removed = Arrays.asList(URLS[0]);
        List<String> added = Arrays.asList(URLS[2], URLS[3]);
        service.uploadSubscriptions(USER, RADIO, subscriptions);
        service.uploadChanges(USER, RADIO, added, removed);
    }

    public void testGetSubscriptionChanges() throws GpodnetServiceException {
        authenticate();
        service.getSubscriptionChanges(USER, RADIO, 1362322610L);
    }

    public void testGetSubscriptionsOfUser()
            throws GpodnetServiceException {
        authenticate();
        service.getSubscriptionsOfUser(USER);
    }

    public void testGetSubscriptionsOfDevice()
            throws GpodnetServiceException {
        authenticate();
        service.getSubscriptionsOfDevice(USER, RADIO);
    }

    public void testConfigureDevices() throws GpodnetServiceException {
        authenticate();
        service.configureDevice(USER, "foo", "This is an updated caption",
                GpodnetDevice.DeviceType.LAPTOP);
    }

    public void testGetDevices() throws GpodnetServiceException {
        authenticate();
        service.getDevices(USER);
    }

    public void testGetSuggestions() throws GpodnetServiceException {
        authenticate();
        service.getSuggestions(10);
    }

    public void testTags() throws GpodnetServiceException {
        service.getTopTags(20);
    }

    public void testPodcastForTags() throws GpodnetServiceException {
        List<GpodnetTag> tags = service.getTopTags(20);
        service.getPodcastsForTag(tags.get(1),
                10);
    }

    public void testSearch() throws GpodnetServiceException {
        service.searchPodcasts("linux", 64);
    }

    public void testToplist() throws GpodnetServiceException {
        service.getPodcastToplist(10);
    }
}
