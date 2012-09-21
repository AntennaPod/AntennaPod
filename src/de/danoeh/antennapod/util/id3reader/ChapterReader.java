package de.danoeh.antennapod.util.id3reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.feed.ID3Chapter;
import de.danoeh.antennapod.util.id3reader.model.FrameHeader;
import de.danoeh.antennapod.util.id3reader.model.TagHeader;

public class ChapterReader extends ID3Reader {

	private static final String FRAME_ID_CHAPTER = "CHAP";
	private static final String FRAME_ID_TITLE = "TIT2";

	private List<ID3Chapter> chapters;
	private ID3Chapter currentChapter;

	@Override
	public int onStartTagHeader(TagHeader header) {
		chapters = new ArrayList<ID3Chapter>();
		System.out.println(header.toString());
		return ID3Reader.ACTION_DONT_SKIP;
	}

	@Override
	public int onStartFrameHeader(FrameHeader header, InputStream input)
			throws IOException, ID3ReaderException {
		System.out.println(header.toString());
		if (header.getId().equals(FRAME_ID_CHAPTER)) {
			if (currentChapter != null) {
				if (!hasId3Chapter(currentChapter)) {
					chapters.add(currentChapter);
					currentChapter = null;
				}
			}
			System.out.println("Found chapter");
			String elementId = readString(input, Integer.MAX_VALUE);
			char[] startTimeSource = readBytes(input, 4);
			long startTime = ((int) startTimeSource[0] << 24)
					| ((int) startTimeSource[1] << 16)
					| ((int) startTimeSource[2] << 8) | startTimeSource[3];
			currentChapter = new ID3Chapter(elementId, startTime);
			skipBytes(input, 12);
			return ID3Reader.ACTION_DONT_SKIP;
		} else if (header.getId().equals(FRAME_ID_TITLE)) {
			if (currentChapter != null && currentChapter.getTitle() == null) {
				System.out.println("Found title");
				skipBytes(input, 1);
				currentChapter
						.setTitle(readString(input, header.getSize() - 1));
				return ID3Reader.ACTION_DONT_SKIP;
			}
		}

		return super.onStartFrameHeader(header, input);
	}

	private boolean hasId3Chapter(ID3Chapter chapter) {
		for (ID3Chapter c : chapters) {
			if (c.getId3ID().equals(chapter.getId3ID())) {
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
		System.out.println("Reached end of tag");
		if (chapters != null) {
			for (ID3Chapter c : chapters) {
				System.out.println(c.toString());
			}
		}
	}

	@Override
	public void onNoTagHeaderFound() {
		System.out.println("No tag header found");
		super.onNoTagHeaderFound();
	}

}
