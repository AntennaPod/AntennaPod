package de.danoeh.antennapod.core.service.download.handler;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadRequest;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.parser.feed.FeedHandler;
import de.danoeh.antennapod.parser.feed.FeedHandlerResult;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;
import de.danoeh.antennapod.model.download.DownloadError;
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
    private DownloadResult downloadResult;
    private boolean successful = true;

    public FeedParserTask(DownloadRequest request) {
        this.request = request;
        downloadResult = new DownloadResult(
        0, request.getTitle(), 0, request.getFeedfileType(), false,
                DownloadError.ERROR_REQUEST_ERROR, new Date(),
                "Unknown error: Status not set");
    }

    @Override
    public FeedHandlerResult call() {
        Feed feed = new Feed(request.getSource(), request.getLastModified());
        feed.setFile_url(request.getDestination());
        feed.setId(request.getFeedfileId());
        feed.setDownloaded(true);
        feed.setPreferences(new FeedPreferences(0, true, FeedPreferences.AutoDeleteAction.GLOBAL,
                VolumeAdaptionSetting.OFF, FeedPreferences.NewEpisodesAction.GLOBAL, request.getUsername(),
                request.getPassword()));
        feed.setPageNr(request.getArguments().getInt(DownloadRequest.REQUEST_ARG_PAGE_NR, 0));

        DownloadError reason = null;
        String reasonDetailed = null;
        FeedHandler feedHandler = new FeedHandler();

        FeedHandlerResult result = null;
        try {
            result = feedHandler.parseFeed(feed);
            Log.d(TAG, feed.getTitle() + " parsed");
            checkFeedData(feed);
            if (TextUtils.isEmpty(feed.getImageUrl())) {
                feed.setImageUrl(Feed.PREFIX_GENERATIVE_COVER + feed.getDownload_url());
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
            if ("html".equalsIgnoreCase(e.getRootElement())) {
                reason = DownloadError.ERROR_UNSUPPORTED_TYPE_HTML;
            }
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
            downloadResult = new DownloadResult(feed, feed.getHumanReadableIdentifier(), DownloadError.SUCCESS,
                                                successful, reasonDetailed);
            return result;
        } else {
            downloadResult = new DownloadResult(feed, feed.getHumanReadableIdentifier(), reason,
                                                successful, reasonDetailed);
            return null;
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Checks if the feed was parsed correctly.
     */
    private void checkFeedData(Feed feed) throws InvalidFeedException {
        if (feed.getTitle() == null) {
            throw new InvalidFeedException("Feed has no title");
        }
        checkFeedItems(feed);
    }

    private void checkFeedItems(Feed feed) throws InvalidFeedException  {
        for (FeedItem item : feed.getItems()) {
            if (item.getTitle() == null) {
                throw new InvalidFeedException("Item has no title: " + item);
            }
        }
    }

    @NonNull
    public DownloadResult getDownloadStatus() {
        return downloadResult;
    }
}
