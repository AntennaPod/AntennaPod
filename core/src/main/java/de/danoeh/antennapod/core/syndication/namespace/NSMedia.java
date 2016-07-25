package de.danoeh.antennapod.core.syndication.namespace;

import android.text.TextUtils;
import android.util.Log;

import org.xml.sax.Attributes;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
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

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
										  Attributes attributes) {
		if (CONTENT.equals(localName)) {
			String url = attributes.getValue(DOWNLOAD_URL);
			String type = attributes.getValue(MIME_TYPE);
			String defaultStr = attributes.getValue(DEFAULT);
			boolean validType;
			boolean isDefault = false;
			if (SyndTypeUtils.enclosureTypeValid(type)) {
				validType = true;
			} else {
				type = SyndTypeUtils.getValidMimeTypeFromUrl(url);
				validType = type != null;
			}

			if (defaultStr == "true")
				isDefault = true;

			if (state.getCurrentItem() != null &&
					(state.getCurrentItem().getMedia() == null || isDefault) &&
					url != null && validType) {
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
			}
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {

	}
}

