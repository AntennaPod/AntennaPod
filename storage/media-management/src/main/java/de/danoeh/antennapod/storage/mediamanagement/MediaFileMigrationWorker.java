package de.danoeh.antennapod.storage.mediamanagement;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.FileNameGenerator;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.i18n.R;
import de.danoeh.antennapod.ui.notifications.MediaOperationChannels;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MediaFileMigrationWorker extends Worker {
    private static final String TAG = "MediaMigrationWorker";
    private static final String KEY_NEW_PATH = "new_path";
    private static final int NOTIFICATION_ID = 1001;

    public MediaFileMigrationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * Enqueues a media file migration task to move all media files to a new storage location.
     *
     * @param context The application context
     * @param newPath The target directory path for migration
     */
    public static void enqueue(Context context, String newPath) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MediaFileMigrationWorker.class)
                .setInputData(new Data.Builder().putString(KEY_NEW_PATH, newPath).build())
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }

    @NonNull
    @Override
    public Result doWork() {
        String newBasePath = getInputData().getString(KEY_NEW_PATH);
        if (newBasePath == null) {
            return Result.failure();
        }

        MediaOperationChannels.createMigrationChannel(getApplicationContext());
        showProgressNotification(getApplicationContext().getString(R.string.migration_starting), 0);

        try {
            boolean success = migrateMediaFiles(newBasePath);
            showCompletionNotification(success);
            return success ? Result.success() : Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "Migration failed", e);
            showCompletionNotification(false);
            return Result.failure();
        }
    }

    /**
     * Migrates all media files to the new base path.
     *
     * @param newBasePath The target directory for migration
     * @return true if migration was successful, false otherwise
     */
    private boolean migrateMediaFiles(String newBasePath) {
        List<FeedMedia> allMedia = DBReader.getAllMediaWithLocalFileUrl();
        int total = allMedia.size();

        // Check available space before starting migration
        if (!hasEnoughSpace(allMedia, newBasePath)) {
            Log.e(TAG, "Insufficient storage space for migration");
            return false;
        }

        for (int i = 0; i < allMedia.size(); i++) {
            FeedMedia media = allMedia.get(i);
            if (!moveMediaFile(media, newBasePath)) {
                Log.e(TAG, "Failed to move media file: " + media.getLocalFileUrl());
                return false;
            }

            int progress = (i + 1) * 100 / total;
            setProgressAsync(new Data.Builder().putInt("progress", progress).build());
            showProgressNotification(getApplicationContext()
                    .getString(R.string.migration_moving_files, progress), progress);
        }

        UserPreferences.setDataFolder(newBasePath);
        return true;
    }

    /**
     * Checks if there is enough storage space for migration.
     *
     * @param mediaFiles  List of media files to migrate
     * @param newBasePath Target directory path
     * @return true if sufficient space is available, false otherwise
     */
    private boolean hasEnoughSpace(List<FeedMedia> mediaFiles, String newBasePath) {
        File targetDir = new File(newBasePath);

        // Ensure target directory exists and is writable
        if (!isWritable(targetDir)) {
            Log.e(TAG, "Target directory not writable: " + newBasePath);
            return false;
        }

        long availableSpace = getAvailableSpace(targetDir);
        long requiredSpace = 0;

        // Calculate total size of files to migrate
        for (FeedMedia media : mediaFiles) {
            if (media.getLocalFileUrl() == null) {
                continue;
            }

            File mediaFile = new File(media.getLocalFileUrl());
            if (mediaFile.exists()) {
                requiredSpace += mediaFile.length();

                // Add transcript file size if exists
                File transcriptFile = new File(media.getLocalFileUrl() + ".transcript");
                if (transcriptFile.exists()) {
                    requiredSpace += transcriptFile.length();
                }
            }
        }

        // Add 10% buffer for safety
        long requiredWithBuffer = (long) (requiredSpace * 1.1);

        Log.d(TAG, "Required space: " + (requiredWithBuffer / 1024 / 1024) + " MB");
        Log.d(TAG, "Available space: " + (availableSpace / 1024 / 1024) + " MB");

        return availableSpace > requiredWithBuffer;
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

    private long getAvailableSpace(File dir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                StorageManager storageManager = (StorageManager) getApplicationContext()
                        .getSystemService(Context.STORAGE_SERVICE);
                return storageManager.getAllocatableBytes(storageManager.getUuidForPath(dir));
            } catch (IOException e) {
                // Fall back to traditional method if StorageManager fails
                return dir.getUsableSpace();
            }
        } else {
            return dir.getUsableSpace();
        }
    }

    /**
     * Moves a single media file to the new location.
     *
     * @param media       The media object to move
     * @param newBasePath The target base directory
     * @return true if move was successful, false otherwise
     * @noinspection checkstyle:WhitespaceAround, checkstyle:WhitespaceAround
     */
    private boolean moveMediaFile(FeedMedia media, String newBasePath) {
        if (media.getLocalFileUrl() == null || media.getItem() == null) {
            return true;
        }

        File oldFile = new File(media.getLocalFileUrl());
        String newPath = generateNewPath(media, newBasePath);
        File newFile = new File(newPath);

        // Check if file is already in correct location
        if (oldFile.getAbsolutePath().equals(newFile.getAbsolutePath())) {
            // File already in correct location, just check download state
            updateDownloadStateIfNeeded(media, oldFile);
            try {
                DBWriter.setFeedMedia(media).get();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to update download state for " + media.getEpisodeTitle(), e);
                return false;
            }
        }

        if (!oldFile.exists()) {
            return true;
        }

        try {
            // Step 1: Copy file to new location (don't delete original yet)
            if (newFile.getParentFile().mkdirs()) {
                Log.d(TAG, "Directory already created");
            }
            copyFile(oldFile, newFile);

            // Step 2: Update database with new path and check download state
            media.setLocalFileUrl(newPath);
            updateDownloadStateIfNeeded(media, newFile);
            DBWriter.setFeedMedia(media).get();

            // Step 3: Only after successful database update, delete old file
            if (!oldFile.delete()) {
                Log.w(TAG, "Could not delete old file: " + oldFile.getAbsolutePath());
                // Continue anyway - file is copied and database updated
            }

            // Step 4: Handle transcript file (copy then delete)
            moveTranscriptFileSafely(oldFile.getAbsolutePath(), newPath);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to move " + oldFile.getAbsolutePath(), e);

            // Cleanup: Remove new file if it was created but database update failed
            if (!newFile.delete()) {
                Log.d(TAG, "Copied file not deletable");
            }

            return false;
        }
    }

    /**
     * Updates the download state of media if the file exists but the episode is not marked as downloaded.
     * This handles cases where files were moved/restored but the database state wasn't updated.
     *
     * @param media     The FeedMedia object to check and potentially update
     * @param mediaFile The physical file that exists
     */
    private void updateDownloadStateIfNeeded(FeedMedia media, File mediaFile) {
        if (!media.isDownloaded() && mediaFile.exists() && mediaFile.length() > 0) {
            // File exists but episode is not marked as downloaded - update the state
            media.setDownloaded(true, System.currentTimeMillis());
            Log.d(TAG, "Updated download state for episode: " + media.getEpisodeTitle()
                    + " (file exists but was not marked as downloaded)");
        }
    }

    /**
     * Safely moves a transcript file from old to new location.
     *
     * @param oldMediaPath The original media file path
     * @param newMediaPath The new media file path
     */
    private void moveTranscriptFileSafely(String oldMediaPath, String newMediaPath) {
        File oldTranscript = new File(oldMediaPath + ".transcript");
        if (!oldTranscript.exists()) {
            return;
        }

        File newTranscript = new File(newMediaPath + ".transcript");
        try {
            // Copy transcript file
            copyFile(oldTranscript, newTranscript);

            // Delete old transcript file
            if (!oldTranscript.delete()) {
                Log.w(TAG, "Could not delete old transcript file: "
                        + oldTranscript.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to move transcript file", e);
            // Don't fail the whole operation for transcript files
        }
    }

    /**
     * Copies a file from source to destination using streams.
     *
     * @param source The source file
     * @param dest   The destination file
     * @throws IOException if copy operation fails
     */
    private void copyFile(File source, File dest) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Generates the new file path for a media file in the target directory.
     *
     * @param media       The media object
     * @param newBasePath The new base directory path
     * @return The complete new file path
     */
    private String generateNewPath(FeedMedia media, String newBasePath) {
        String feedTitle = FileNameGenerator.generateFileName(
                media.getItem().getFeed().getTitle());
        String mediaPath = "media/" + feedTitle;
        String fileName = new File(media.getLocalFileUrl()).getName();

        return new File(new File(newBasePath, mediaPath), fileName).getAbsolutePath();
    }

    private void showProgressNotification(String message, int progress) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                MediaOperationChannels.MIGRATION_CHANNEL_ID)
                .setContentTitle(getApplicationContext().getString(R.string.migration_moving_media_files))
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setProgress(100, progress, false)
                .setOngoing(true);

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void showCompletionNotification(boolean success) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String title = success ? getApplicationContext().getString(R.string.migration_completed)
                : getApplicationContext().getString(R.string.migration_failed);
        String message = success ? getApplicationContext().getString(R.string.migration_completed_message)
                : getApplicationContext().getString(R.string.migration_failed_message);
        int icon = success ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_notify_error;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                MediaOperationChannels.MIGRATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify(NOTIFICATION_ID + 1, builder.build());
    }
}
