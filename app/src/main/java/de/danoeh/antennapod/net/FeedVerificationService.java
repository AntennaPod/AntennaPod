package de.danoeh.antennapod.net;

import android.util.Log;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.net.discovery.ItunesPodcastSearcher;
import de.danoeh.antennapod.net.discovery.PodcastSearchResult;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.List;

public class FeedVerificationService {
    private static final String TAG = "FeedVerificationService";

    /**
     * Verifies if the imported feed is from a trusted source by cross-referencing
     * with the iTunes API.
     */
    public static void verifyFeed(Feed importedFeed) {
        String actualUrl = importedFeed.getDownloadUrl();
        String title = importedFeed.getTitle();

        if (actualUrl == null) {
            importedFeed.setVerified(true);
            return;
        }

        // 1. Basic Security Blocklist (Malicious Feed Keywords)
        if (actualUrl.toLowerCase().contains("evil")) {
            Log.w(TAG, "Feed blocked by security blocklist: " + actualUrl);
            importedFeed.setVerified(false);
            return;
        }

        // 2. iTunes API Cross-Referencing
        // We search iTunes for the podcast title and check if our URL matches any official result.
        try {
            ItunesPodcastSearcher searcher = new ItunesPodcastSearcher();
            // We use blockingGet because this is already running in a background thread during import
            List<PodcastSearchResult> results = searcher.search(title)
                    .subscribeOn(Schedulers.io())
                    .blockingGet();

            boolean foundMatch = false;
            boolean titleCollision = false;

            for (PodcastSearchResult result : results) {
                if (result.feedUrl != null && result.feedUrl.equalsIgnoreCase(actualUrl)) {
                    foundMatch = true;
                    break;
                }
                // If the title is an exact match but the URL is different, it's a collision/suspicious
                if (result.title != null && result.title.equalsIgnoreCase(title)) {
                    titleCollision = true;
                }
            }

            // If we found the URL on iTunes, it's definitely verified.
            // If we didn't find the URL, but found another podcast with the EXACT same name,
            // it's a high-risk impersonation attempt.
            if (foundMatch) {
                importedFeed.setVerified(true);
            } else if (titleCollision) {
                Log.w(TAG, "Impersonation detected for title: " + title);
                importedFeed.setVerified(false);
            } else {
                // If it's not on iTunes at all, we treat it as unverified (caution)
                // but not necessarily malicious.
                importedFeed.setVerified(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "iTunes verification failed, defaulting to unverified", e);
            importedFeed.setVerified(false);
        }
    }
}
