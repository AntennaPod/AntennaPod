package de.danoeh.antennapod.core.syndication.namespace;

import android.text.TextUtils;
import android.util.Log;

import org.xml.sax.Attributes;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import de.danoeh.antennapod.core.syndication.namespace.atom.AtomText;
import de.danoeh.antennapod.core.syndication.util.SyndTypeUtils;

/** Processes tags from the http://search.yahoo.com/mrss/ namespace. */
public class NSMedia extends Namespace {
	private static final String TAG = "NSMedia";

	public static final String NSTAG = "media";
	public static final String NSURI = "http://search.yahoo.com/mrss/";

	private static final String CONTENT = "content";
	private static final String DOWNLOAD_URL = "url";
	private static final String SIZE = "fileSize";
	private static final String MIME_TYPE = "type";
	private static final String DURATION = "duration";
	private static final String DEFAULT = "isDefault";
	private static final String MEDIUM = "medium";

	private static final String MEDIUM_IMAGE = "image";
	private static final String MEDIUM_AUDIO = "audio";
	private static final String MEDIUM_VIDEO = "video";

	private static final String IMAGE = "thumbnail";
	private static final String IMAGE_URL = "url";

	private static final String DESCRIPTION = "description";
	private static final String DESCRIPTION_TYPE = "type";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
										  Attributes attributes) {
		if (CONTENT.equals(localName)) {
			String url = attributes.getValue(DOWNLOAD_URL);
			String type = attributes.getValue(MIME_TYPE);
			String defaultStr = attributes.getValue(DEFAULT);
			String medium = attributes.getValue(MEDIUM);
			boolean validTypeMedia = false;
			boolean validTypeImage = false;

			boolean isDefault = "true".equals(defaultStr);

			if (MEDIUM_AUDIO.equals(medium) || MEDIUM_VIDEO.equals(medium)) {
				validTypeMedia = true;
			} else if (MEDIUM_IMAGE.equals(medium)) {
				validTypeImage = true;
			} else {
				if (type == null) {
					type = SyndTypeUtils.getMimeTypeFromUrl(url);
				}

				if (SyndTypeUtils.enclosureTypeValid(type)) {
					validTypeMedia = true;
				} else if (SyndTypeUtils.imageTypeValid(type)) {
					validTypeImage = true;
				}
			}

			if (state.getCurrentItem() != null &&
					(state.getCurrentItem().getMedia() == null || isDefault) &&
					url != null && validTypeMedia) {
				long size = 0;
				String sizeStr = attributes.getValue(SIZE);
				try {
					size = Long.parseLong(sizeStr);
				} catch (NumberFormatException e) {
					Log.e(TAG, "Size \"" + sizeStr + "\" could not be parsed.");
				}

				int durationMs = 0;
				String durationStr = attributes.getValue(DURATION);
				if (!TextUtils.isEmpty(durationStr)) {
					try {
						long duration = Long.parseLong(durationStr);
						durationMs = (int) TimeUnit.MILLISECONDS.convert(duration, TimeUnit.SECONDS);
					} catch (NumberFormatException e) {
						Log.e(TAG, "Duration \"" + durationStr + "\" could not be parsed");
					}
				}
				FeedMedia media = new FeedMedia(state.getCurrentItem(), url, size, type);
				if (durationMs > 0) {
					media.setDuration(durationMs);
				}
				state.getCurrentItem().setMedia(media);
			} else if (state.getCurrentItem() != null && url != null && validTypeImage) {
				FeedImage image = new FeedImage();
				image.setDownload_url(url);
				image.setOwner(state.getCurrentItem());

				state.getCurrentItem().setImage(image);
			}
		} else if (IMAGE.equals(localName)) {
			String url = attributes.getValue(IMAGE_URL);
			if (url != null) {
				FeedImage image = new FeedImage();
				image.setDownload_url(url);

				if (state.getCurrentItem() != null) {
					image.setOwner(state.getCurrentItem());
					state.getCurrentItem().setImage(image);
				} else {
					if (state.getFeed().getImage() == null) {
						image.setOwner(state.getFeed());
						state.getFeed().setImage(image);
					}
				}
			}
		} else if (DESCRIPTION.equals(localName)) {
			String type = attributes.getValue(DESCRIPTION_TYPE);
			return new AtomText(localName, this, type);
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (DESCRIPTION.equals(localName)) {
			String content = state.getContentBuf().toString();
			if (state.getCurrentItem() != null && content != null &&
				state.getCurrentItem().getDescription() == null) {
					state.getCurrentItem().setDescription(content);
			}
		}
	}
}

