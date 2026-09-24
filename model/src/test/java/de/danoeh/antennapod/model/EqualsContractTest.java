package de.danoeh.antennapod.model;

import de.danoeh.antennapod.model.download.DownloadRequest;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.RemoteMedia;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class EqualsContractTest {
    @Test public void downloadRequest() { EqualsVerifier.forClass(DownloadRequest.class).verify(); }
    @Test public void embeddedChapterImage() { EqualsVerifier.forClass(EmbeddedChapterImage.class).verify(); }
    @Test public void feed() { EqualsVerifier.forClass(Feed.class).verify(); }
    @Test public void feedItem() { EqualsVerifier.forClass(FeedItem.class).verify(); }
    @Test public void feedMedia() { EqualsVerifier.forClass(FeedMedia.class).verify(); }
    @Test public void remoteMedia() { EqualsVerifier.forClass(RemoteMedia.class).verify(); }
}
