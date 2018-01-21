package de.danoeh.antennapod.core.feed;

import android.database.Cursor;

import java.io.File;

import de.danoeh.antennapod.core.asynctask.ImageResource;
import de.danoeh.antennapod.core.storage.PodDBAdapter;


public class FeedImage extends FeedFile implements ImageResource {
	public static final int FEEDFILETYPE_FEEDIMAGE = 1;

	private String title;
	private FeedComponent owner;

	public FeedImage(FeedComponent owner, String download_url, String title) {
		super(null, download_url, false);
		this.download_url = download_url;
		this.title = title;
        this.owner = owner;
	}

	public FeedImage(long id, String title, String file_url,
			String download_url, boolean downloaded) {
		super(file_url, download_url, downloaded);
		this.id = id;
		this.title = title;
	}

    public FeedImage() {
        super();
    }

	public static FeedImage fromCursor(Cursor cursor) {
		int indexId = cursor.getColumnIndex(PodDBAdapter.KEY_ID);
		int indexTitle = cursor.getColumnIndex(PodDBAdapter.KEY_TITLE);
		int indexFileUrl = cursor.getColumnIndex(PodDBAdapter.KEY_FILE_URL);
		int indexDownloadUrl = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOAD_URL);
		int indexDownloaded = cursor.getColumnIndex(PodDBAdapter.KEY_DOWNLOADED);

		return new FeedImage(
				cursor.getLong(indexId),
				cursor.getString(indexTitle),
				cursor.getString(indexFileUrl),
				cursor.getString(indexDownloadUrl),
				cursor.getInt(indexDownloaded) > 0
		);
	}


	@Override
	public String getHumanReadableIdentifier() {
		if (owner != null && owner.getHumanReadableIdentifier() != null) {
			return owner.getHumanReadableIdentifier();
		} else {
			return download_url;
		}
	}

	@Override
	public int getTypeAsInt() {
		return FEEDFILETYPE_FEEDIMAGE;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public FeedComponent getOwner() {
		return owner;
	}

	public void setOwner(FeedComponent owner) {
		this.owner = owner;
	}

    @Override
    public String getImageLocation() {
        if (file_url != null && downloaded) {
            return new File(file_url).getAbsolutePath();
        } else if(download_url != null) {
            return download_url;
        } else {
            return null;
        }
    }
}
