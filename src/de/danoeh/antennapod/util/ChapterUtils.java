package de.danoeh.antennapod.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

	/**
	 * Uses the download URL of a media object of a feeditem to read its ID3
	 * chapters.
	 */
	public static void readID3ChaptersFromFeedMediaDownloadUrl(FeedItem item) {
		if (AppConfig.DEBUG)
			Log.d(TAG, "Reading id3 chapters from item " + item.getTitle());
		final FeedMedia media = item.getMedia();
		if (media != null && media.getDownload_url() != null) {
			InputStream in = null;
			try {
				URL url = new URL(media.getDownload_url());
				ChapterReader reader = new ChapterReader();

				in = url.openStream();
				reader.readInputStream(in);
				List<Chapter> chapters = reader.getChapters();

				if (chapters != null) {
					Collections
							.sort(chapters, new ChapterStartTimeComparator());
					processChapters(chapters, item);
					if (chaptersValid(chapters)) {
						item.setChapters(chapters);
						Log.i(TAG, "Chapters loaded");
					} else {
						Log.e(TAG, "Chapter data was invalid");
					}
				} else {
					Log.i(TAG, "ChapterReader could not find any ID3 chapters");
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
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
			Log.e(TAG,
					"Unable to read ID3 chapters: media or download URL was null");
		}
	}

	/**
	 * Uses the file URL of a media object of a feeditem to read its ID3
	 * chapters.
	 */
	public static void readID3ChaptersFromFeedMediaFileUrl(FeedItem item) {
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
						Collections.sort(chapters,
								new ChapterStartTimeComparator());
						processChapters(chapters, item);
						if (chaptersValid(chapters)) {
							item.setChapters(chapters);
							Log.i(TAG, "Chapters loaded");
						} else {
							Log.e(TAG, "Chapter data was invalid");
						}
					} else {
						Log.i(TAG,
								"ChapterReader could not find any ID3 chapters");
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
		if (chapters.isEmpty()) {
			return false;
		}
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
