package de.danoeh.antennapod.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.feed.Chapter;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.util.comparator.ChapterStartTimeComparator;
import de.danoeh.antennapod.util.id3reader.ChapterReader;
import de.danoeh.antennapod.util.id3reader.ID3ReaderException;

/** Utility class for getting chapter data from media files. */
public class ChapterUtils {
	private static final String TAG = "ChapterUtils";

	private ChapterUtils() {
	}

	public static void readID3ChaptersFromFeedItem(FeedItem item) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Reading id3 chapters from item " + item.getTitle());
		final FeedMedia media = item.getMedia();
		if (media != null && media.isDownloaded()
				&& media.getFile_url() != null) {
			File source = new File(media.getFile_url());
			if (source.exists()) {
				ChapterReader reader = new ChapterReader();
				InputStream in = null;

				try {
					in = new BufferedInputStream(new FileInputStream(source));
					reader.readInputStream(in);
					List<Chapter> chapters = reader.getChapters();
					
					if (chapters != null) {
						Collections.sort(chapters, new ChapterStartTimeComparator());
						processChapters(chapters, item);
						if (chaptersValid(chapters)) {
							item.setChapters(chapters);
						} else {
							Log.e(TAG, "Chapter data was invalid");
						}
					} else {
						Log.i(TAG, "ChapterReader could not find any ID3 chapters");
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ID3ReaderException e) {
					e.printStackTrace();
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				Log.e(TAG, "Unable to read id3 chapters: Source doesn't exist");
			}
		}
	}

	/** Makes sure that chapter does a title and an item attribute. */
	private static void processChapters(List<Chapter> chapters, FeedItem item) {
		for (int i = 0; i < chapters.size(); i++) {
			Chapter c = chapters.get(i);
			if (c.getTitle() == null) {
				c.setTitle(Integer.toString(i));
			}
			c.setItem(item);
		}
	}

	private static boolean chaptersValid(List<Chapter> chapters) {
		for (Chapter c : chapters) {
			if (c.getTitle() == null) {
				return false;
			}
			if (c.getStart() < 0) {
				return false;
			}
		}
		return true;
	}
	
}
