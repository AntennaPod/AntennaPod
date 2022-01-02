package de.test.antennapod.util.syndication.feedgenerator;

import java.io.IOException;
import java.io.OutputStream;

import de.danoeh.antennapod.model.feed.Feed;

/**
 * Generates a machine-readable, platform-independent representation of a Feed object.
 */
public interface FeedGenerator {

    /**
     * Creates a machine-readable, platform-independent representation of a given
     * Feed object and writes it to the given OutputStream.
     * <p/>
     * The representation might not be compliant with its specification if the feed
     * is missing certain attribute values. This is intentional because the FeedGenerator is
     * used for creating test data.
     *
     * @param feed         The feed that should be written. Must not be null.
     * @param outputStream The output target that the feed will be written to. The outputStream is not closed after
     *                     the method's execution Must not be null.
     * @param encoding     The encoding to use. Must not be null.
     * @param flags        Optional argument for enabling implementation-dependent features.
     */
    void writeFeed(Feed feed, OutputStream outputStream, String encoding, long flags) throws IOException;
}
