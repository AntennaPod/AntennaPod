package de.danoeh.antennapod.model;

import android.os.Bundle;

import de.danoeh.antennapod.model.download.DownloadRequest;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.RemoteMedia;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

@RunWith(RobolectricTestRunner.class)
public class EqualsContractTest {
    private static Feed feed(long id) {
        return new Feed(id, null, "title" + id, "http://example.com/" + id, null, null, null, null, null,
                null, null, null, "http://example.com/feed" + id, 0);
    }

    private static FeedItem item(long id) {
        return new FeedItem(id, "item" + id, "item" + id, "http://example.com/item" + id, new Date(),
                FeedItem.UNPLAYED, feed(id));
    }

    private static Bundle bundle(String value) {
        Bundle bundle = new Bundle();
        bundle.putString("key", value);
        return bundle;
    }

    @Test
    public void feed() {
        EqualsVerifier.forClass(Feed.class)
                .usingGetClass()
                .withPrefabValues(FeedItem.class, item(1), item(2))
                .withOnlyTheseFields("id")
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void feedItem() {
        EqualsVerifier.forClass(FeedItem.class)
                .usingGetClass()
                .withPrefabValues(Feed.class, feed(1), feed(2))
                .withOnlyTheseFields("id")
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void feedMedia() {
        EqualsVerifier.forClass(FeedMedia.class)
                .usingGetClass()
                .withPrefabValues(FeedItem.class, item(1), item(2))
                .withOnlyTheseFields("id")
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void remoteMedia() {
        EqualsVerifier.forClass(RemoteMedia.class)
                .withOnlyTheseFields("downloadUrl", "feedUrl", "itemIdentifier")
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void embeddedChapterImage() {
        EqualsVerifier.forClass(EmbeddedChapterImage.class)
                .usingGetClass()
                .withPrefabValues(FeedMedia.class, new FeedMedia(1, null, 0, 0, 0, "", "", "a", 0, null, 0, 0),
                        new FeedMedia(2, null, 0, 0, 0, "", "", "b", 0, null, 0, 0))
                .withOnlyTheseFields("imageUrl")
                .withNonnullFields("imageUrl")
                .verify();
    }

    @Test
    public void downloadRequest() {
        EqualsVerifier.forClass(DownloadRequest.class)
                .withPrefabValues(Bundle.class, bundle("a"), bundle("b"))
                .withIgnoredFields("arguments")
                .withNonnullFields("destination", "source", "arguments")
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS, Warning.STRICT_HASHCODE)
                .verify();
    }
}
