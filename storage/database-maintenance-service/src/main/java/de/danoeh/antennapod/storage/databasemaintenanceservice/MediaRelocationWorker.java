package de.danoeh.antennapod.storage.databasemaintenanceservice;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaRelocationWorker extends Worker {
    private static final String TAG = "MediaRelocationWorker";
    private static final int NOTIFICATION_ID = 1002;

    public MediaRelocationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * Enqueues a media relocation task to find and update paths of missing media files.
     * 
     * @param context The application context
     */
    public static void enqueue(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MediaRelocationWorker.class).build();
        WorkManager.getInstance(context).enqueue(request);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationUtils.createChannels(getApplicationContext());
        showProgressNotification(getApplicationContext().getString(R.string.relocation_starting), 0);

        try {
            int relocatedCount = relocateMediaFiles();
            showCompletionNotification(true, relocatedCount);

            Data result = new Data.Builder()
                    .putInt("relocated_count", relocatedCount)
                    .build();

            return Result.success(result);
        } catch (Exception e) {
            Log.e(TAG, "Relocation failed", e);
            showCompletionNotification(false, 0);
            return Result.failure();
        }
    }

    /**
     * Relocates media files by searching for them in all available storage locations.
     * 
     * @return The number of successfully relocated files
     */
    private int relocateMediaFiles() {
        List<FeedMedia> allMedia = DBReader.getAllMediaWithLocalFileUrl();
        List<File> searchLocations = getAllStorageLocations();
        int relocatedCount = 0;
        int total = allMedia.size();

        for (int i = 0; i < allMedia.size(); i++) {
            FeedMedia media = allMedia.get(i);
            if (media.getLocalFileUrl() == null) {
                continue;
            }

            File currentFile = new File(media.getLocalFileUrl());
            if (currentFile.exists()) {
                // File exists at current path, check download state
                if (updateDownloadStateIfNeeded(media, currentFile)) {
                    try {
                        DBWriter.setFeedMedia(media).get();
                        Log.d(TAG, "Updated download state for: " + currentFile.getName());
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to update download state", e);
                    }
                }
            } else {
                // Search across all storage locations with priority
                String newPath = findMediaFileInAllLocations(searchLocations, media.getLocalFileUrl());
                if (newPath != null) {
                    try {
                        media.setLocalFileUrl(newPath);
                        updateDownloadStateIfNeeded(media, new File(newPath));
                        DBWriter.setFeedMedia(media).get();
                        relocatedCount++;
                        Log.d(TAG, "Relocated: " + currentFile.getName() + " -> " + newPath);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to update media path", e);
                    }
                }
            }

            // Update progress
            int progress = (i + 1) * 100 / total;
            setProgressAsync(new Data.Builder().putInt("progress", progress).build());
            showProgressNotification(getApplicationContext()
                    .getString(R.string.relocation_checking_files, progress), progress);
        }

        return relocatedCount;
    }

    /**
     * Updates the download state of media if the file exists but the episode is not marked as downloaded.
     * This handles cases where files were moved/restored but the database state wasn't updated.
     *
     * @param media     The FeedMedia object to check and potentially update
     * @param mediaFile The physical file that exists
     * @return true if state was updated, false if no update was needed
     */
    private boolean updateDownloadStateIfNeeded(FeedMedia media, File mediaFile) {
        if (!media.isDownloaded() && mediaFile.exists() && mediaFile.length() > 0) {
            // File exists but episode is not marked as downloaded - update the state
            media.setDownloaded(true, System.currentTimeMillis());
            Log.d(TAG, "Updated download state for episode: " + media.getEpisodeTitle()
                    + " (file exists but was not marked as downloaded)");
            return true;
        }
        return false;
    }

    /**
     * Gets all available storage locations for searching media files.
     * 
     * @return List of directories to search in priority order
     */
    private List<File> getAllStorageLocations() {
        final List<File> mediaDirs = new ArrayList<>();
        mediaDirs.addAll(List.of(getApplicationContext().getExternalFilesDirs(null)));
        mediaDirs.addAll(List.of(getApplicationContext().getExternalMediaDirs()));
        final List<File> entries = new ArrayList<>(mediaDirs.size());
        for (File dir : mediaDirs) {
            if (!isWritable(dir)) {
                continue;
            }
            entries.add(new File(dir.getAbsolutePath()));
        }
        if (entries.isEmpty() && isWritable(getApplicationContext().getFilesDir())) {
            entries.add(new File(getApplicationContext().getFilesDir().getAbsolutePath()));
        }
        return entries;
    }

    /**
     * Checks if a directory is writable.
     * 
     * @param dir The directory to check
     * @return true if directory exists and is writable, false otherwise
     */
    private boolean isWritable(File dir) {
        return dir != null && dir.exists() && dir.canRead() && dir.canWrite();
    }

    /**
     * Searches for a media file in all available storage locations.
     * 
     * @param searchLocations List of directories to search
     * @param originalPath The original file path
     * @return The new path if found, null otherwise
     */
    private String findMediaFileInAllLocations(List<File> searchLocations, String originalPath) {
        String relativePath = extractRelativePath(originalPath);
        String fileName = new File(originalPath).getName();

        Log.d(TAG, "Looking for file: " + fileName + ", relative path: " + relativePath);

        // Search in priority order
        for (File location : searchLocations) {
            if (!location.exists()) {
                continue;
            }

            // Try suffix matching first (most reliable)
            if (relativePath != null) {
                File targetFile = new File(location, relativePath);
                if (targetFile.exists()) {
                    Log.d(TAG, "Found by suffix match: " + targetFile.getAbsolutePath());
                    return targetFile.getAbsolutePath();
                }
            }

            // Try filename search in this location
            String foundPath = findFileRecursive(location, fileName);
            if (foundPath != null) {
                Log.d(TAG, "Found by filename match in " + location.getAbsolutePath()
                        + ": " + foundPath);
                return foundPath;
            }
        }

        Log.d(TAG, "File not found: " + fileName);
        return null;
    }

    /**
     * Extracts the relative path (feedname/filename) from a full file path.
     * 
     * @param originalPath The original file path
     * @return The relative path or null if extraction fails
     */
    private String extractRelativePath(String originalPath) {
        // Path structure: <something>/<feedname>/<episode filename>
        // Extract the last two path components: feedname/filename
        
        String[] pathParts = originalPath.split("/");
        if (pathParts.length >= 2) {
            String feedName = pathParts[pathParts.length - 2];
            String fileName = pathParts[pathParts.length - 1];
            return feedName + "/" + fileName;
        }
        
        return null;
    }

    /**
     * Recursively searches for a file by name in a directory.
     * 
     * @param dir The directory to search
     * @param fileName The file name to find
     * @return The full path if found, null otherwise
     */
    private String findFileRecursive(File dir, String fileName) {
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        // First check current directory
        for (File file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                return file.getAbsolutePath();
            }
        }

        // Then search subdirectories
        for (File file : files) {
            if (file.isDirectory()) {
                String result = findFileRecursive(file, fileName);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private void showProgressNotification(String message, int progress) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                NotificationUtils.CHANNEL_ID_RELOCATION)
                .setContentTitle(getApplicationContext().getString(R.string.relocation_checking_media_files))
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, false)
                .setOngoing(true);

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void showCompletionNotification(boolean success, int relocatedCount) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String title = success ? getApplicationContext().getString(R.string.relocation_completed)
                : getApplicationContext().getString(R.string.relocation_failed);
        String message = success ? getApplicationContext()
                .getString(R.string.relocation_completed_message, relocatedCount)
                : getApplicationContext().getString(R.string.relocation_failed_message);
        int icon = success ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_notify_error;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                NotificationUtils.CHANNEL_ID_RELOCATION)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(icon)
                .setAutoCancel(true);

        manager.notify(NOTIFICATION_ID + 1, builder.build());
    }
}
