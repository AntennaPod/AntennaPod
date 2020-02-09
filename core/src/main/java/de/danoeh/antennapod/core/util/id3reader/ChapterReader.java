package de.danoeh.antennapod.core.util.id3reader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.ID3Chapter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.id3reader.model.FrameHeader;
import de.danoeh.antennapod.core.util.id3reader.model.TagHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

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
                char[] startTimeSource = readChars(input, 4);
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
                    try {
                        String decodedLink = URLDecoder.decode(link.toString(), "UTF-8");
                        currentChapter.setLink(decodedLink);
                        Log.d(TAG, "Found link: " + currentChapter.getLink());
                    } catch (IllegalArgumentException iae) {
                        Log.w(TAG, "Bad URL found in ID3 data");
                    }

                    return ID3Reader.ACTION_DONT_SKIP;
                }
                break;
            case "APIC":
                Log.d(TAG, header.toString());
                StringBuilder mime = new StringBuilder();
                int read = readString(mime, input, header.getSize());
                byte type = (byte) input.read();
                // $00  Other
                // $01  32x32 pixels 'file icon' (PNG only)
                // $02  Other file icon
                // $03  Cover (front)
                // $04  Cover (back)
                // $05  Leaflet page
                // $06  Media (e.g. label side of CD)
                // $07  Lead artist/lead performer/soloist
                // $08  Artist/performer
                // $09  Conductor
                // $0A  Band/Orchestra
                // $0B  Composer
                // $0C  Lyricist/text writer
                // $0D  Recording Location
                // $0E  During recording
                // $0F  During performance
                // $10  Movie/video screen capture
                // $11  A bright coloured fish
                // $12  Illustration
                // $13  Band/artist logotype
                // $14  Publisher/Studio logotype
                read++;
                StringBuilder description = new StringBuilder();
                read += readISOString(description, input, header.getSize()); // Should use encoding from first string


                Log.d(TAG, "Found apic: " + mime + "," + description);
                if (mime.toString().equals("-->")) {
                    // Data contains a link to a picture
                    StringBuilder link = new StringBuilder();
                    readISOString(link, input, header.getSize());
                    Log.d(TAG, "link: " + link);
                } else {
                    // Data contains the picture
                    byte[] imageData = readBytes(input, header.getSize() - read);

                    Bitmap bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                    try (FileOutputStream out = new FileOutputStream(new File(UserPreferences.getDataFolder(null),
                            "chapter" + chapters.size() + ".jpg"))) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                        // PNG is a lossless format, the compression factor (100) is ignored
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return ID3Reader.ACTION_DONT_SKIP;
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
