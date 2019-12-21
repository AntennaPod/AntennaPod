package de.danoeh.antennapod.core.service.download.handler;

import android.util.Log;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import de.danoeh.antennapod.core.syndication.handler.FeedHandler;
import de.danoeh.antennapod.core.syndication.handler.FeedHandlerResult;
import de.danoeh.antennapod.core.syndication.handler.UnsupportedFeedtypeException;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.InvalidFeedException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;

public class FeedParserTask implements Callable<FeedHandlerResult> {
    private static final String TAG = "FeedParserTask";
    private final DownloadRequest request;
    private DownloadStatus downloadStatus;
    private boolean successful = true;

    public FeedParserTask(DownloadRequest request) {
        this.request = request;
    }

    @Override
    public FeedHandlerResult call() {
        Feed feed = new Feed(request.getSource(), request.getLastModified());
        feed.setFile_url(request.getDestination());
        feed.setId(request.getFeedfileId());
        feed.setDownloaded(true);
        feed.setPreferences(new FeedPreferences(0, true, FeedPreferences.AutoDeleteAction.GLOBAL,
                VolumeAdaptionSetting.OFF, request.getUsername(), request.getPassword()));
        feed.setPageNr(request.getArguments().getInt(DownloadRequester.REQUEST_ARG_PAGE_NR, 0));

        DownloadError reason = null;
        String reasonDetailed = null;
        FeedHandler feedHandler = new FeedHandler();

        FeedHandlerResult result = null;
        try {
            result = feedHandler.parseFeed(feed);
            Log.d(TAG, feed.getTitle() + " parsed");
            if (!checkFeedData(feed)) {
                throw new InvalidFeedException();
            }

        } catch (SAXException | IOException | ParserConfigurationException e) {
            successful = false;
            e.printStackTrace();
            reason = DownloadError.ERROR_PARSER_EXCEPTION;
            reasonDetailed = e.getMessage();
        } catch (UnsupportedFeedtypeException e) {
            e.printStackTrace();
            successful = false;
            reason = DownloadError.ERROR_UNSUPPORTED_TYPE;
            reasonDetailed = e.getMessage();
        } catch (InvalidFeedException e) {
            e.printStackTrace();
            successful = false;
            reason = DownloadError.ERROR_PARSER_EXCEPTION;
            reasonDetailed = e.getMessage();
        } finally {
            File feedFile = new File(request.getDestination());
            if (feedFile.exists()) {
                boolean deleted = feedFile.delete();
                Log.d(TAG, "Deletion of file '" + feedFile.getAbsolutePath() + "' "
                        + (deleted ? "successful" : "FAILED"));
            }
        }

        if (successful) {
            downloadStatus = new DownloadStatus(feed, feed.getHumanReadableIdentifier(),
                    DownloadError.SUCCESS, successful, reasonDetailed);
            return result;
        } else {
            downloadStatus = new DownloadStatus(feed, feed.getHumanReadableIdentifier(),
                    reason, successful, reasonDetailed);
            return null;
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Checks if the feed was parsed correctly.
     */
    private boolean checkFeedData(Feed feed) {
        if (feed.getTitle() == null) {
            Log.e(TAG, "Feed has no title.");
            return false;
        }
        if (!hasValidFeedItems(feed)) {
            Log.e(TAG, "Feed has invalid items");
            return false;
        }
        return true;
    }

    private boolean hasValidFeedItems(Feed feed) {
        for (FeedItem item : feed.getItems()) {
            if (item.getTitle() == null) {
                Log.e(TAG, "Item has no title");
                return false;
            }
            if (item.getPubDate() == null) {
                Log.e(TAG, "Item has no pubDate. Using current time as pubDate");
                if (item.getTitle() != null) {
                    Log.e(TAG, "Title of invalid item: " + item.getTitle());
                }
                item.setPubDate(new Date());
            }
        }
        return true;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }
}
