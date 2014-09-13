/*
 * FFmpegMediaMetadataRetriever: A unified interface for retrieving frame 
 * and meta data from an input media file.
 *
 * Copyright 2014 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Changes by Daniel Oeh:
 * - Rewrite of the 'static' section
 * - Addition of 'getChapters' method
 *
 */

package wseemann.media;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * FFmpegMediaMetadataRetriever class provides a unified interface for retrieving
 * frame and meta data from an input media file.
 */
public class FFmpegMediaMetadataRetriever
{
	private final static String TAG = "FFmpegMediaMetadataRetriever";

    public static boolean LIB_AVAILABLE = false;

	/**
	 * User defined bitmap configuration. A bitmap configuration describes how pixels are
	 * stored. This affects the quality (color depth) as well as the ability to display
	 * transparent/translucent colors. 
	 */
	public static Bitmap.Config IN_PREFERRED_CONFIG;
	
	@SuppressLint("SdCardPath")
	private static final String LIBRARY_PATH = "/data/data/";
	
	private static final String [] JNI_LIBRARIES = {
		"avutil",
		"swscale",
		"avcodec",
		"avformat",
		"ffmpeg_mediametadataretriever_jni"
	};
	
    static {
        /*
    	StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    	
    	StringBuffer path = null;
    	File file = null;
    	boolean foundLibs = false;
    	
    	for (int j = 0; j < stackTraceElements.length; j++) {
    		String libraryPath = stackTraceElements[j].getClassName();
    	
    		String [] packageFragments = libraryPath.trim().split("\\.");
    	
    		path = new StringBuffer(LIBRARY_PATH);
    	
    		for (int i = 0; i < packageFragments.length; i++) {
    			if (i > 0) {
    				path.append(".");
    			}
    		
    			path.append(packageFragments[i]);
    			try {
    				//System.load(path.toString() + "/lib/" + JNI_LIBRARIES[0]);
    				file = new File(path.toString() + "/lib/" + JNI_LIBRARIES[0]);
    				if (file.exists()) {
    					path.append("/lib/");
    					foundLibs = true;
    					break;
    				}
    			} catch (UnsatisfiedLinkError ex) {
    			}
    		}
    		
    		if (foundLibs) {
    			break;
    		}
    	}

        // Since libraries for some architectures have been excluded from the source in order to save
        // space, this class might not work on all devices.
    	if (!foundLibs) {
    		Log.e(TAG, TAG + " libraries not found. Did you forget to add them to your libs folder?");
    		//throw new UnsatisfiedLinkError();
            LIB_AVAILABLE = false;
    	} else {
            LIB_AVAILABLE = true;
            for (int i = 0; i < JNI_LIBRARIES.length; i++) {
                System.load(path.toString() + JNI_LIBRARIES[i]);
            }

            native_init();
        }*/
        try {
            for (int i = 0; i < JNI_LIBRARIES.length; i++) {
                System.loadLibrary(JNI_LIBRARIES[i]);
            }
            LIB_AVAILABLE = true;
            native_init();
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Library not found");
            LIB_AVAILABLE = false;
        }
    }

    // The field below is accessed by native methods
    private int mNativeContext;
    
    public FFmpegMediaMetadataRetriever() {
    	native_setup();
    }

    /**
     * Sets the data source (file pathname) to use. Call this
     * method before the rest of the methods in this class. This method may be
     * time-consuming.
     * 
     * @param path The path of the input media file.
     * @throws IllegalArgumentException If the path is invalid.
     */
    public native void setDataSource(String path) throws IllegalArgumentException;
    
    /**
     * Sets the data source (URI) to use. Call this
     * method before the rest of the methods in this class. This method may be
     * time-consuming.
     *
     * @param uri The URI of the input media.
     * @param headers the headers to be sent together with the request for the data
     * @throws IllegalArgumentException If the URI is invalid.
     */
    public void setDataSource(String uri,  Map<String, String> headers)
            throws IllegalArgumentException {
        int i = 0;
        String[] keys = new String[headers.size()];
        String[] values = new String[headers.size()];
        for (Map.Entry<String, String> entry: headers.entrySet()) {
            keys[i] = entry.getKey();
            values[i] = entry.getValue();
            ++i;
        }
        _setDataSource(uri, keys, values);
    }

    private native void _setDataSource(
        String uri, String[] keys, String[] values)
        throws IllegalArgumentException;

    /**
     * Sets the data source (FileDescriptor) to use.  It is the caller's
     * responsibility to close the file descriptor. It is safe to do so as soon
     * as this call returns. Call this method before the rest of the methods in
     * this class. This method may be time-consuming.
     * 
     * @param fd the FileDescriptor for the file you want to play
     * @param offset the offset into the file where the data to be played starts,
     * in bytes. It must be non-negative
     * @param length the length in bytes of the data to be played. It must be
     * non-negative.
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public native void setDataSource(FileDescriptor fd, long offset, long length)
            throws IllegalArgumentException;
    
    /**
     * Sets the data source (FileDescriptor) to use. It is the caller's
     * responsibility to close the file descriptor. It is safe to do so as soon
     * as this call returns. Call this method before the rest of the methods in
     * this class. This method may be time-consuming.
     * 
     * @param fd the FileDescriptor for the file you want to play
     * @throws IllegalArgumentException if the FileDescriptor is invalid
     */
    public void setDataSource(FileDescriptor fd)
            throws IllegalArgumentException {
        // intentionally less than LONG_MAX
        setDataSource(fd, 0, 0x7ffffffffffffffL);
    }
    
    /**
     * Sets the data source as a content Uri. Call this method before 
     * the rest of the methods in this class. This method may be time-consuming.
     * 
     * @param context the Context to use when resolving the Uri
     * @param uri the Content URI of the data you want to play
     * @throws IllegalArgumentException if the Uri is invalid
     * @throws SecurityException if the Uri cannot be used due to lack of
     * permission.
     */
    public void setDataSource(Context context, Uri uri)
        throws IllegalArgumentException, SecurityException {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        
        String scheme = uri.getScheme();
        if(scheme == null || scheme.equals("file")) {
            setDataSource(uri.getPath());
            return;
        }

        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            try {
                fd = resolver.openAssetFileDescriptor(uri, "r");
            } catch(FileNotFoundException e) {
                throw new IllegalArgumentException();
            }
            if (fd == null) {
                throw new IllegalArgumentException();
            }
            FileDescriptor descriptor = fd.getFileDescriptor();
            if (!descriptor.valid()) {
                throw new IllegalArgumentException();
            }
            // Note: using getDeclaredLength so that our behavior is the same
            // as previous versions when the content provider is returning
            // a full file.
            if (fd.getDeclaredLength() < 0) {
                setDataSource(descriptor);
            } else {
                setDataSource(descriptor, fd.getStartOffset(), fd.getDeclaredLength());
            }
            return;
        } catch (SecurityException ex) {
        } finally {
            try {
                if (fd != null) {
                    fd.close();
                }
            } catch(IOException ioEx) {
            }
        }
        setDataSource(uri.toString());
    }
    
    /**
     * Call this method after setDataSource(). This method retrieves the 
     * meta data value associated with the keyCode.
     * 
     * The keyCode currently supported is listed below as METADATA_XXX
     * constants. With any other value, it returns a null pointer.
     * 
     * @param keyCode One of the constants listed below at the end of the class.
     * @return The meta data value associate with the given keyCode on success; 
     * null on failure.
     */
    public native String extractMetadata(String key);

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame close to the given time position by considering
     * the given option if possible, and returns it as a bitmap. This is
     * useful for generating a thumbnail for an input data source or just
     * obtain and display a frame at the given time position.
     *
     * @param timeUs The time position where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarantee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @param option a hint on how the frame is found. Use
     * {@link #OPTION_PREVIOUS_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp earlier than or the same as timeUs. Use
     * {@link #OPTION_NEXT_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp later than or the same as timeUs. Use
     * {@link #OPTION_CLOSEST_SYNC} if one wants to retrieve a sync frame
     * that has a timestamp closest to or the same as timeUs. Use
     * {@link #OPTION_CLOSEST} if one wants to retrieve a frame that may
     * or may not be a sync frame but is closest to or the same as timeUs.
     * {@link #OPTION_CLOSEST} often has larger performance overhead compared
     * to the other options if there is no sync frame located at timeUs.
     *
     * @return A Bitmap containing a representative video frame, which 
     *         can be null, if such a frame cannot be retrieved.
     */
    public Bitmap getFrameAtTime(long timeUs, int option) {
        if (option < OPTION_PREVIOUS_SYNC ||
            option > OPTION_CLOSEST) {
            throw new IllegalArgumentException("Unsupported option: " + option);
        }

    	Bitmap b = null;
    	
        BitmapFactory.Options bitmapOptionsCache = new BitmapFactory.Options();
        bitmapOptionsCache.inPreferredConfig = getInPreferredConfig();
        bitmapOptionsCache.inDither = false;
    	
        byte [] picture = _getFrameAtTime(timeUs, option);
        
        if (picture != null) {
        	b = BitmapFactory.decodeByteArray(picture, 0, picture.length, bitmapOptionsCache);
        }
        
        return b;
    }

    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame close to the given time position if possible,
     * and returns it as a bitmap. This is useful for generating a thumbnail
     * for an input data source. Call this method if one does not care
     * how the frame is found as long as it is close to the given time;
     * otherwise, please call {@link #getFrameAtTime(long, int)}.
     *
     * @param timeUs The time position where the frame will be retrieved.
     * When retrieving the frame at the given time position, there is no
     * guarentee that the data source has a frame located at the position.
     * When this happens, a frame nearby will be returned. If timeUs is
     * negative, time position and option will ignored, and any frame
     * that the implementation considers as representative may be returned.
     *
     * @return A Bitmap containing a representative video frame, which
     *         can be null, if such a frame cannot be retrieved.
     *
     * @see #getFrameAtTime(long, int)
     */
    public Bitmap getFrameAtTime(long timeUs) {
    	Bitmap b = null;
    	
        BitmapFactory.Options bitmapOptionsCache = new BitmapFactory.Options();
        bitmapOptionsCache.inPreferredConfig = getInPreferredConfig();
        bitmapOptionsCache.inDither = false;
    	
        byte [] picture = _getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);
        
        if (picture != null) {
        	b = BitmapFactory.decodeByteArray(picture, 0, picture.length, bitmapOptionsCache);
        }
        
        return b;
    }
    
    /**
     * Call this method after setDataSource(). This method finds a
     * representative frame at any time position if possible,
     * and returns it as a bitmap. This is useful for generating a thumbnail
     * for an input data source. Call this method if one does not
     * care about where the frame is located; otherwise, please call
     * {@link #getFrameAtTime(long)} or {@link #getFrameAtTime(long, int)}
     *
     * @return A Bitmap containing a representative video frame, which
     *         can be null, if such a frame cannot be retrieved.
     *
     * @see #getFrameAtTime(long)
     * @see #getFrameAtTime(long, int)
     */
    public Bitmap getFrameAtTime() {
        return getFrameAtTime(-1, OPTION_CLOSEST_SYNC);
    }

    /** 
     * Call this method after setDataSource(). This method finds any
     * chapter marks that are contained in the media file.
     *
     * @return An array of FFmpegChapter objects or null if no chapters
     * could be found.
     * */
    public native FFmpegChapter[] getChapters();
    
    private native byte [] _getFrameAtTime(long timeUs, int option);
    
    /**
     * Call this method after setDataSource(). This method finds the optional
     * graphic or album/cover art associated associated with the data source. If
     * there are more than one pictures, (any) one of them is returned.
     * 
     * @return null if no such graphic is found.
     */
    public native byte[] getEmbeddedPicture();
    
    /**
     * Call it when one is done with the object. This method releases the memory
     * allocated internally.
     */
    public native void release();
    private native void native_setup();
    private static native void native_init();

    private native final void native_finalize();

    @Override
    protected void finalize() throws Throwable {
        try {
            native_finalize();
        } finally {
            super.finalize();
        }
    }

    private Bitmap.Config getInPreferredConfig() {
    	if (IN_PREFERRED_CONFIG != null) {
    		return IN_PREFERRED_CONFIG;
    	}
    	
    	return Bitmap.Config.RGB_565;
    }
    
    /**
     * Option used in method {@link #getFrameAtTime(long, int)} to get a
     * frame at a specified location.
     *
     * @see #getFrameAtTime(long, int)
     */
    /* Do not change these option values without updating their counterparts
     * in jni/metadata/ffmpeg_mediametadataretriever.h!
     */
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * right before or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_PREVIOUS_SYNC    = 0x00;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * right after or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_NEXT_SYNC        = 0x01;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a sync (or key) frame associated with a data source that is located
     * closest to (in time) or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_CLOSEST_SYNC     = 0x02;
    /**
     * This option is used with {@link #getFrameAtTime(long, int)} to retrieve
     * a frame (not necessarily a key frame) associated with a data source that
     * is located closest to or at the given time.
     *
     * @see #getFrameAtTime(long, int)
     */
    public static final int OPTION_CLOSEST          = 0x03;

    /**
     * The metadata key to retrieve the name of the set this work belongs to.
     */
    public static final String METADATA_KEY_ALBUM = "album";
    /**
     * The metadata key to retrieve the main creator of the set/album, if different 
     * from artist. e.g. "Various Artists" for compilation albums.
     */
    public static final String METADATA_KEY_ALBUM_ARTIST = "album_artist";
    /**
     * The metadata key to retrieve the main creator of the work.
     */
    public static final String METADATA_KEY_ARTIST = "artist";
    /**
     * The metadata key to retrieve the any additional description of the file.
     */
    public static final String METADATA_KEY_COMMENT = "comment";
    /**
     * The metadata key to retrieve the who composed the work, if different from artist.
     */
    public static final String METADATA_KEY_COMPOSER = "composer";
    /**
     * The metadata key to retrieve the name of copyright holder.
     */
    public static final String METADATA_KEY_COPYRIGHT = "copyright";
    /**
     * The metadata key to retrieve the date when the file was created, preferably in ISO 8601.
     */
    public static final String METADATA_KEY_CREATION_TIME = "creation_time";
    /**
     * The metadata key to retrieve the date when the work was created, preferably in ISO 8601.
     */
    public static final String METADATA_KEY_DATE = "date";
    /**
     * The metadata key to retrieve the number of a subset, e.g. disc in a multi-disc collection.
     */
    public static final String METADATA_KEY_DISC = "disc";
    /**
     * The metadata key to retrieve the name/settings of the software/hardware that produced the file.
     */
    public static final String METADATA_KEY_ENCODER = "encoder";
    /**
     * The metadata key to retrieve the person/group who created the file.
     */
    public static final String METADATA_KEY_ENCODED_BY = "encoded_by";
    /**
     * The metadata key to retrieve the original name of the file.
     */
    public static final String METADATA_KEY_FILENAME = "filename";
    /**
     * The metadata key to retrieve the genre of the work.
     */
    public static final String METADATA_KEY_GENRE = "genre";
    /**
     * The metadata key to retrieve the main language in which the work is performed, preferably
     * in ISO 639-2 format. Multiple languages can be specified by separating them with commas.
     */
    public static final String METADATA_KEY_LANGUAGE = "language";
    /**
     * The metadata key to retrieve the artist who performed the work, if different from artist.
     * E.g for "Also sprach Zarathustra", artist would be "Richard Strauss" and performer "London 
     * Philharmonic Orchestra".
     */
    public static final String METADATA_KEY_PERFORMER = "performer";
    /**
     * The metadata key to retrieve the name of the label/publisher.
     */
    public static final String METADATA_KEY_PUBLISHER = "publisher";
    /**
     * The metadata key to retrieve the name of the service in broadcasting (channel name).
     */
    public static final String METADATA_KEY_SERVICE_NAME = "service_name";
    /**
     * The metadata key to retrieve the name of the service provider in broadcasting.
     */
    public static final String METADATA_KEY_SERVICE_PROVIDER = "service_provider";
    /**
     * The metadata key to retrieve the name of the work.
     */
    public static final String METADATA_KEY_TITLE = "title";
    /**
     * The metadata key to retrieve the number of this work in the set, can be in form current/total.
     */
    public static final String METADATA_KEY_TRACK = "track";
    /**
     * The metadata key to retrieve the total bitrate of the bitrate variant that the current stream 
     * is part of.
     */
    public static final String METADATA_KEY_VARIANT_BITRATE = "bitrate";
    /**
     * The metadata key to retrieve the duration of the work in milliseconds.
     */
    public static final String METADATA_KEY_DURATION = "duration";
    /**
     * The metadata key to retrieve the audio codec of the work.
     */
    public static final String METADATA_KEY_AUDIO_CODEC = "audio_codec";
    /**
     * The metadata key to retrieve the video codec of the work.
     */
    public static final String METADATA_KEY_VIDEO_CODEC = "video_codec";
    /**
     * This key retrieves the video rotation angle in degrees, if available.
     * The video rotation angle may be 0, 90, 180, or 270 degrees.
     */
    public static final String METADATA_KEY_VIDEO_ROTATION = "rotate";
    /**
     * The metadata key to retrieve the main creator of the work.
     */
    public static final String METADATA_KEY_ICY_METADATA = "icy_metadata";
    /**
     * The metadata key to retrieve the main creator of the work.
     */
    //private static final String METADATA_KEY_ICY_ARTIST = "icy_artist";
    /**
     * The metadata key to retrieve the name of the work.
     */
    //private static final String METADATA_KEY_ICY_TITLE = "icy_title";
    /**
     * This metadata key retrieves the average framerate (in frames/sec), if available.
     */
    public static final String METADATA_KEY_FRAMERATE = "framerate";
}
