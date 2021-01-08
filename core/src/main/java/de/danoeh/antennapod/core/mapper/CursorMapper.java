package de.danoeh.antennapod.core.mapper;

import android.database.Cursor;

import java.util.Date;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.feed.FeedFilter;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.feed.ID3Chapter;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.core.feed.VorbisCommentChapter;
import de.danoeh.antennapod.core.storage.PodDBAdapter;
import de.danoeh.antennapod.core.util.SortOrder;

public class CursorMapper {

    public static Chapter fromCursorToChapter(Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexTitle = cursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
        int indexStart = cursor.getColumnIndex(PodDBAdapter.KEY_START);
        int indexLink = cursor.getColumnIndex(PodDBAdapter.KEY_LINK);
        int indexImage = cursor.getColumnIndex(PodDBAdapter.KEY_IMAGE_URL);
        int indexChapterType = cursor.getColumnIndex(PodDBAdapter.KEY_CHAPTER_TYPE);

        long id = cursor.getLong(indexId);
        String title = cursor.getString(indexTitle);
        long start = cursor.getLong(indexStart);
        String link = cursor.getString(indexLink);
        String imageUrl = cursor.getString(indexImage);
        int chapterType = cursor.getInt(indexChapterType);

        Chapter chapter = null;
        switch (chapterType) {
            case SimpleChapter.CHAPTERTYPE_SIMPLECHAPTER:
                chapter = new SimpleChapter(start, title, link, imageUrl);
                break;
            case ID3Chapter.CHAPTERTYPE_ID3CHAPTER:
                chapter = new ID3Chapter(start, title, link, imageUrl);
                break;
            case VorbisCommentChapter.CHAPTERTYPE_VORBISCOMMENT_CHAPTER:
                chapter = new VorbisCommentChapter(start, title, link, imageUrl);
                break;
        }
        chapter.setId(id);
        return chapter;
    }

    public static Feed fromCursorToFeed(Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexLastUpdate = cursor.getColumnIndex(PodDBAdapter.KEY_LASTUPDATE);
        int indexTitle = cursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
        int indexCustomTitle = cursor.getColumnIndex(PodDBAdapter.KEY_CUSTOM_TITLE);
        int indexLink = cursor.getColumnIndex(PodDBAdapter.KEY_LINK);
        int indexDescription = cursor.getColumnIndex(PodDBAdapter.KEY_DESCRIPTION);
        int indexPaymentLink = cursor.getColumnIndex(PodDBAdapter.KEY_PAYMENT_LINK);
        int indexAuthor = cursor.getColumnIndex(PodDBAdapter.KEY_AUTHOR);
        int indexLanguage = cursor.getColumnIndex(PodDBAdapter.KEY_LANGUAGE);
        int indexType = cursor.getColumnIndex(PodDBAdapter.KEY_TYPE);
        int indexFeedIdentifier = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_IDENTIFIER);
        int indexFileUrl = cursor.getColumnIndex(PodDBAdapter.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL);
        int indexDownloaded = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED);
        int indexIsPaged = cursor.getColumnIndex(PodDBAdapter.KEY_IS_PAGED);
        int indexNextPageLink = cursor.getColumnIndex(PodDBAdapter.KEY_NEXT_PAGE_LINK);
        int indexHide = cursor.getColumnIndex(PodDBAdapter.KEY_HIDE);
        int indexSortOrder = cursor.getColumnIndex(PodDBAdapter.KEY_SORT_ORDER);
        int indexLastUpdateFailed = cursor.getColumnIndex(PodDBAdapter.KEY_LAST_UPDATE_FAILED);
        int indexImageUrl = cursor.getColumnIndex(PodDBAdapter.KEY_IMAGE_URL);

        Feed feed = new Feed(
                cursor.getLong(indexId),
                cursor.getString(indexLastUpdate),
                cursor.getString(indexTitle),
                cursor.getString(indexCustomTitle),
                cursor.getString(indexLink),
                cursor.getString(indexDescription),
                cursor.getString(indexPaymentLink),
                cursor.getString(indexAuthor),
                cursor.getString(indexLanguage),
                cursor.getString(indexType),
                cursor.getString(indexFeedIdentifier),
                cursor.getString(indexImageUrl),
                cursor.getString(indexFileUrl),
                cursor.getString(indexDownloadUrl),
                cursor.getInt(indexDownloaded) > 0,
                cursor.getInt(indexIsPaged) > 0,
                cursor.getString(indexNextPageLink),
                cursor.getString(indexHide),
                SortOrder.fromCodeString(cursor.getString(indexSortOrder)),
                cursor.getInt(indexLastUpdateFailed) > 0
        );

        FeedPreferences preferences = CursorMapper.fromCursorToFeedPreferences(cursor);
        feed.setPreferences(preferences);
        return feed;
    }

    public static FeedItem fromCursorToFeedItem(Cursor cursor) {
        int indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_ITEM_ID);
        int indexTitle = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_TITLE);
        int indexLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LINK);
        int indexPubDate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PUBDATE);
        int indexPaymentLink = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PAYMENT_LINK);
        int indexFeedId = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FEED);
        int indexHasChapters = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_HAS_CHAPTERS);
        int indexRead = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_READ);
        int indexItemIdentifier = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_ITEM_IDENTIFIER);
        int indexAutoDownload = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_AUTO_DOWNLOAD);
        int indexImageUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_IMAGE_URL);

        long id = cursor.getInt(indexId);
        String title = cursor.getString(indexTitle);
        String link = cursor.getString(indexLink);
        Date pubDate = new Date(cursor.getLong(indexPubDate));
        String paymentLink = cursor.getString(indexPaymentLink);
        long feedId = cursor.getLong(indexFeedId);
        boolean hasChapters = cursor.getInt(indexHasChapters) > 0;
        int state = cursor.getInt(indexRead);
        String itemIdentifier = cursor.getString(indexItemIdentifier);
        long autoDownload = cursor.getLong(indexAutoDownload);
        String imageUrl = cursor.getString(indexImageUrl);

        return new FeedItem(id, title, link, pubDate, paymentLink, feedId,
                hasChapters, imageUrl, state, itemIdentifier, autoDownload);
    }

    public static FeedMedia fromCursorToMedia(Cursor cursor) {
        int indexId = cursor.getColumnIndexOrThrow(PodDBAdapter.SELECT_KEY_MEDIA_ID);
        int indexPlaybackCompletionDate = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PLAYBACK_COMPLETION_DATE);
        int indexDuration = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DURATION);
        int indexPosition = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_POSITION);
        int indexSize = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_SIZE);
        int indexMimeType = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_MIME_TYPE);
        int indexFileUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOAD_URL);
        int indexDownloaded = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_DOWNLOADED);
        int indexPlayedDuration = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_PLAYED_DURATION);
        int indexLastPlayedTime = cursor.getColumnIndexOrThrow(PodDBAdapter.KEY_LAST_PLAYED_TIME);

        long mediaId = cursor.getLong(indexId);
        Date playbackCompletionDate = null;
        long playbackCompletionTime = cursor.getLong(indexPlaybackCompletionDate);
        if (playbackCompletionTime > 0) {
            playbackCompletionDate = new Date(playbackCompletionTime);
        }

        Boolean hasEmbeddedPicture;
        switch(cursor.getInt(cursor.getColumnIndex(PodDBAdapter.KEY_HAS_EMBEDDED_PICTURE))) {
            case 1:
                hasEmbeddedPicture = Boolean.TRUE;
                break;
            case 0:
                hasEmbeddedPicture = Boolean.FALSE;
                break;
            default:
                hasEmbeddedPicture = null;
                break;
        }

        return new FeedMedia(
                mediaId,
                null,
                cursor.getInt(indexDuration),
                cursor.getInt(indexPosition),
                cursor.getLong(indexSize),
                cursor.getString(indexMimeType),
                cursor.getString(indexFileUrl),
                cursor.getString(indexDownloadUrl),
                cursor.getInt(indexDownloaded) > 0,
                playbackCompletionDate,
                cursor.getInt(indexPlayedDuration),
                hasEmbeddedPicture,
                cursor.getLong(indexLastPlayedTime)
        );
    }

    public static FeedPreferences fromCursorToFeedPreferences(Cursor cursor) {
        int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
        int indexAutoDownload = cursor.getColumnIndex(PodDBAdapter.KEY_AUTO_DOWNLOAD);
        int indexAutoRefresh = cursor.getColumnIndex(PodDBAdapter.KEY_KEEP_UPDATED);
        int indexAutoDeleteAction = cursor.getColumnIndex(PodDBAdapter.KEY_AUTO_DELETE_ACTION);
        int indexVolumeAdaption = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_VOLUME_ADAPTION);
        int indexUsername = cursor.getColumnIndex(PodDBAdapter.KEY_USERNAME);
        int indexPassword = cursor.getColumnIndex(PodDBAdapter.KEY_PASSWORD);
        int indexIncludeFilter = cursor.getColumnIndex(PodDBAdapter.KEY_INCLUDE_FILTER);
        int indexExcludeFilter = cursor.getColumnIndex(PodDBAdapter.KEY_EXCLUDE_FILTER);
        int indexFeedPlaybackSpeed = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_PLAYBACK_SPEED);
        int indexAutoSkipIntro = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_SKIP_INTRO);
        int indexAutoSkipEnding = cursor.getColumnIndex(PodDBAdapter.KEY_FEED_SKIP_ENDING);

        long feedId = cursor.getLong(indexId);
        boolean autoDownload = cursor.getInt(indexAutoDownload) > 0;
        boolean autoRefresh = cursor.getInt(indexAutoRefresh) > 0;
        int autoDeleteActionIndex = cursor.getInt(indexAutoDeleteAction);
        FeedPreferences.AutoDeleteAction autoDeleteAction = FeedPreferences.AutoDeleteAction.values()[autoDeleteActionIndex];
        int volumeAdaptionValue = cursor.getInt(indexVolumeAdaption);
        VolumeAdaptionSetting volumeAdaptionSetting = VolumeAdaptionSetting.fromInteger(volumeAdaptionValue);
        String username = cursor.getString(indexUsername);
        String password = cursor.getString(indexPassword);
        String includeFilter = cursor.getString(indexIncludeFilter);
        String excludeFilter = cursor.getString(indexExcludeFilter);
        float feedPlaybackSpeed = cursor.getFloat(indexFeedPlaybackSpeed);
        int feedAutoSkipIntro = cursor.getInt(indexAutoSkipIntro);
        int feedAutoSkipEnding = cursor.getInt(indexAutoSkipEnding);
        return new FeedPreferences(feedId,
                autoDownload,
                autoRefresh,
                autoDeleteAction,
                volumeAdaptionSetting,
                username,
                password,
                new FeedFilter(includeFilter, excludeFilter),
                feedPlaybackSpeed,
                feedAutoSkipIntro,
                feedAutoSkipEnding
        );
    }



}
