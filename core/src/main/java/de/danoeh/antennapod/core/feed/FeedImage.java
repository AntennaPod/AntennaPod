package de.danoeh.antennapod.core.feed;

import android.net.Uri;

import de.danoeh.antennapod.core.asynctask.PicassoImageResource;

import java.io.File;


public class FeedImage extends FeedFile implements PicassoImageResource {
	public static final int FEEDFILETYPE_FEEDIMAGE = 1;

	protected String title;
	protected FeedComponent owner;

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
    public Uri getImageUri() {
        if (file_url != null && downloaded) {
            return Uri.fromFile(new File(file_url));
        } else {
            return null;
        }
    }
}
