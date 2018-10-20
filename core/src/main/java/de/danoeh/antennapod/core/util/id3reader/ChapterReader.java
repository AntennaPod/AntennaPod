package de.danoeh.antennapod.core.util.id3reader;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.ID3Chapter;
import de.danoeh.antennapod.core.util.id3reader.model.FrameHeader;
import de.danoeh.antennapod.core.util.id3reader.model.TagHeader;

public class ChapterReader extends ID3Reader {
    private static final String TAG = "ID3ChapterReader";

	private static final String FRAME_ID_CHAPTER = "CHAP";
	private static final String FRAME_ID_TITLE = "TIT2";
    private static final String FRAME_ID_LINK = "WXXX";

	private List<Chapter> chapters;
	private ID3Chapter currentChapter;

	@Override
	public int onStartTagHeader(TagHeader header) {
		chapters = new ArrayList<>();
		Log.d(TAG, "header: " + header);
		return ID3Reader.ACTION_DONT_SKIP;
	}

	@Override
	public int onStartFrameHeader(FrameHeader header, InputStream input)
			throws IOException, ID3ReaderException {
		Log.d(TAG, "header: " + header);
		switch (header.getId()) {
			case FRAME_ID_CHAPTER:
				if (currentChapter != null) {
					if (!hasId3Chapter(currentChapter)) {
						chapters.add(currentChapter);
						Log.d(TAG, "Found chapter: " + currentChapter);
						currentChapter = null;
					}
				}
				StringBuilder elementId = new StringBuilder();
				readISOString(elementId, input, Integer.MAX_VALUE);
				char[] startTimeSource = readBytes(input, 4);
				long startTime = ((int) startTimeSource[0] << 24)
						| ((int) startTimeSource[1] << 16)
						| ((int) startTimeSource[2] << 8) | startTimeSource[3];
				currentChapter = new ID3Chapter(elementId.toString(), startTime);
				skipBytes(input, 12);
				return ID3Reader.ACTION_DONT_SKIP;
			case FRAME_ID_TITLE:
				if (currentChapter != null && currentChapter.getTitle() == null) {
					StringBuilder title = new StringBuilder();
					readString(title, input, header.getSize());
					currentChapter
							.setTitle(title.toString());
					Log.d(TAG, "Found title: " + currentChapter.getTitle());

					return ID3Reader.ACTION_DONT_SKIP;
				}
				break;
			case FRAME_ID_LINK:
				if (currentChapter != null) {
					// skip description
					int descriptionLength = readString(null, input, header.getSize());
					StringBuilder link = new StringBuilder();
					readISOString(link, input, header.getSize() - descriptionLength);
					String decodedLink = URLDecoder.decode(link.toString(), "UTF-8");

					currentChapter.setLink(decodedLink);

					Log.d(TAG, "Found link: " + currentChapter.getLink());
					return ID3Reader.ACTION_DONT_SKIP;
				}
				break;
			case "APIC":
				Log.d(TAG, header.toString());
				break;
		}

		return super.onStartFrameHeader(header, input);
	}

	private boolean hasId3Chapter(ID3Chapter chapter) {
		for (Chapter c : chapters) {
			if (((ID3Chapter) c).getId3ID().equals(chapter.getId3ID())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onEndTag() {
		if (currentChapter != null) {
			if (!hasId3Chapter(currentChapter)) {
				chapters.add(currentChapter);
			}
		}
		Log.d(TAG, "Reached end of tag");
		if (chapters != null) {
			for (Chapter c : chapters) {
				Log.d(TAG, "chapter: " + c);
			}
		}
	}

	@Override
	public void onNoTagHeaderFound() {
		Log.d(TAG, "No tag header found");
		super.onNoTagHeaderFound();
	}

	public List<Chapter> getChapters() {
		return chapters;
	}

}
