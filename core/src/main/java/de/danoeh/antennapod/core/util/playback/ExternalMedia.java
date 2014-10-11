package de.danoeh.antennapod.core.util.playback;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.MediaType;
import de.danoeh.antennapod.core.util.ChapterUtils;

import java.util.List;
import java.util.concurrent.Callable;

/** Represents a media file that is stored on the local storage device. */
public class ExternalMedia implements Playable {

	public static final int PLAYABLE_TYPE_EXTERNAL_MEDIA = 2;
	public static final String PREF_SOURCE_URL = "ExternalMedia.PrefSourceUrl";
	public static final String PREF_POSITION = "ExternalMedia.PrefPosition";
	public static final String PREF_MEDIA_TYPE = "ExternalMedia.PrefMediaType";

	private String source;

	private String episodeTitle;
	private String feedTitle;
	private MediaType mediaType = MediaType.AUDIO;
	private List<Chapter> chapters;
	private int duration;
	private int position;

	public ExternalMedia(String source, MediaType mediaType) {
		super();
		this.source = source;
		this.mediaType = mediaType;
	}

	public ExternalMedia(String source, MediaType mediaType, int position) {
		this(source, mediaType);
		this.position = position;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(source);
		dest.writeString(mediaType.toString());
		dest.writeInt(position);
	}

	@Override
	public void writeToPreferences(Editor prefEditor) {
		prefEditor.putString(PREF_SOURCE_URL, source);
		prefEditor.putString(PREF_MEDIA_TYPE, mediaType.toString());
		prefEditor.putInt(PREF_POSITION, position);
	}

	@Override
	public void loadMetadata() throws PlayableException {
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		try {
			mmr.setDataSource(source);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new PlayableException(
					"IllegalArgumentException when setting up MediaMetadataReceiver");
		} catch (RuntimeException e) {
			// http://code.google.com/p/android/issues/detail?id=39770
			e.printStackTrace();
			throw new PlayableException(
					"RuntimeException when setting up MediaMetadataRetriever");
		}
		episodeTitle = mmr
				.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		feedTitle = mmr
				.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        try {
		    duration = Integer.parseInt(mmr
				.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new PlayableException("NumberFormatException when reading duration of media file");
        }
		ChapterUtils.loadChaptersFromFileUrl(this);
	}

	@Override
	public void loadChapterMarks() {

	}

	@Override
	public String getEpisodeTitle() {
		return episodeTitle;
	}

	@Override
	public Callable<String> loadShownotes() {
		return new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "";
            }
        };
	}

	@Override
	public List<Chapter> getChapters() {
		return chapters;
	}

	@Override
	public String getWebsiteLink() {
		return null;
	}

	@Override
	public String getPaymentLink() {
		return null;
	}

	@Override
	public String getFeedTitle() {
		return feedTitle;
	}

	@Override
	public Object getIdentifier() {
		return source;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	public MediaType getMediaType() {
		return mediaType;
	}

	@Override
	public String getLocalMediaUrl() {
		return source;
	}

	@Override
	public String getStreamUrl() {
		return null;
	}

	@Override
	public boolean localFileAvailable() {
		return true;
	}

	@Override
	public boolean streamAvailable() {
		return false;
	}

	@Override
	public void saveCurrentPosition(SharedPreferences pref, int newPosition) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt(PREF_POSITION, newPosition);
		position = newPosition;
		editor.commit();
	}

	@Override
	public void setPosition(int newPosition) {
		position = newPosition;
	}

	@Override
	public void setDuration(int newDuration) {
		duration = newDuration;
	}

	@Override
	public void onPlaybackStart() {

	}

	@Override
	public void onPlaybackCompleted() {

	}

	@Override
	public int getPlayableType() {
		return PLAYABLE_TYPE_EXTERNAL_MEDIA;
	}

	@Override
	public void setChapters(List<Chapter> chapters) {
		this.chapters = chapters;
	}

	public static final Parcelable.Creator<ExternalMedia> CREATOR = new Parcelable.Creator<ExternalMedia>() {
		public ExternalMedia createFromParcel(Parcel in) {
			String source = in.readString();
			MediaType type = MediaType.valueOf(in.readString());
			int position = 0;
			if (in.dataAvail() > 0) {
				position = in.readInt();
			}
			ExternalMedia extMedia = new ExternalMedia(source, type, position);
			return extMedia;
		}

		public ExternalMedia[] newArray(int size) {
			return new ExternalMedia[size];
		}
	};

    @Override
    public Uri getImageUri() {
        if (localFileAvailable()) {
            return new Uri.Builder().scheme(SCHEME_MEDIA).encodedPath(getLocalMediaUrl()).build();
        } else {
            return null;
        }
    }
}
