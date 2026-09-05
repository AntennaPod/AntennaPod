package de.danoeh.antennapod.storage.mediamanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.net.download.serviceinterface.FileNameGenerator;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaFileMigrationWorker extends Worker {
    private static final String TAG = "MediaMigrationWorker";
    private static final String KEY_NEW_PATH = "new_path";
    private static final String KEY_ALLOW_PARTIAL = "allow_partial";
    private static final String KEY_SKIP_SPACE_CHECK = "skip_space_check";
    private static final String CHANNEL_ID = "media_migration";
    private static final int NOTIFICATION_ID = 1001;
    
    public enum FailureReason {
        NOT_ENOUGH_SPACE,
        FAILED_TO_MOVE_FILES,
        UNEXPECTED_ERROR,
        PARTIAL_SUCCESS
    }
    
    public MediaFileMigrationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void enqueue(Context context, String newPath, boolean allowPartial) {
        enqueue(context, newPath, allowPartial, false);
    }

    public static void enqueue(Context context, String newPath, boolean allowPartial, boolean skipSpaceCheck) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(MediaFileMigrationWorker.class)
                .setInputData(new Data.Builder()
                        .putString(KEY_NEW_PATH, newPath)
                        .putBoolean(KEY_ALLOW_PARTIAL, allowPartial)
                        .putBoolean(KEY_SKIP_SPACE_CHECK, skipSpaceCheck)
                        .build())
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }

    @NonNull
    @Override
    public Result doWork() {
        String newBasePath = getInputData().getString(KEY_NEW_PATH);
        boolean allowPartial = getInputData().getBoolean(KEY_ALLOW_PARTIAL, false);
        boolean skipSpaceCheck = getInputData().getBoolean(KEY_SKIP_SPACE_CHECK, false);
        if (newBasePath == null) {
            return Result.failure();
        }
        
        createNotificationChannel();
        setForegroundAsync(createForegroundInfo("Starting migration..."));
        
        try {
            FailureReason failureReason = migrateMediaFiles(newBasePath, allowPartial, skipSpaceCheck);
            boolean success = failureReason == null || failureReason == FailureReason.PARTIAL_SUCCESS;
            
            // Only show completion notification if not partial success (partial success shows its own notification)
            if (failureReason != FailureReason.PARTIAL_SUCCESS) {
                showCompletionNotification(success, failureReason);
            }
            
            return success ? Result.success() : Result.failure();
        } catch (Exception e) {
            Log.e(TAG, "Migration failed", e);
            showCompletionNotification(false, FailureReason.UNEXPECTED_ERROR);
            return Result.failure();
        }
    }
    
    private FailureReason migrateMediaFiles(String newBasePath, boolean allowPartial, boolean skipSpaceCheck) {
        // Get all downloaded episodes and extract their media
        List<FeedItem> downloadedItems = DBReader.getEpisodes(0, Integer.MAX_VALUE, 
            new FeedItemFilter(FeedItemFilter.unfiltered()), SortOrder.DATE_NEW_OLD);
        
        List<FeedMedia> allMedia = new ArrayList<>();
        for (FeedItem item : downloadedItems) {
            if (item.getMedia() != null && item.getMedia().getLocalFileUrl() != null) {
                allMedia.add(item.getMedia());
            }
        }
        
        int total = allMedia.size();
        int failedCount = 0;
        
        // Check available space before starting migration (skip if moving within same device or if forced)
        if (!skipSpaceCheck && !isSameDevice(allMedia, newBasePath) && !hasEnoughSpace(allMedia, newBasePath)) {
            Log.e(TAG, "Insufficient storage space for migration");
            if (!allowPartial) {
                return FailureReason.NOT_ENOUGH_SPACE;
            }
            Log.w(TAG, "Continuing with partial migration due to space constraints");
        }
        
        for (int i = 0; i < allMedia.size(); i++) {
            FeedMedia media = allMedia.get(i);
            if (!moveMediaFile(media, newBasePath)) {
                Log.e(TAG, "Failed to move media file: " + media.getLocalFileUrl());
                failedCount++;
                if (!allowPartial) {
                    return FailureReason.FAILED_TO_MOVE_FILES;
                }
                // Continue with next file in partial mode
            }
            
            int progress = (i + 1) * 100 / total;
            setProgressAsync(new Data.Builder().putInt("progress", progress).build());
            setForegroundAsync(
                    createForegroundInfo("Moving files... " + progress + "%")
            );
        }
        
        UserPreferences.setDataFolder(newBasePath);
        
        if (failedCount > 0 && allowPartial) {
            Log.i(TAG, "Partial migration completed. " + failedCount + " files could not be moved");
            showPartialSuccessNotification(total, failedCount);
            return FailureReason.PARTIAL_SUCCESS;
        }
        
        return null; // Complete success
    }
    
    private boolean isSameDevice(List<FeedMedia> mediaFiles, String newBasePath) {
        if (mediaFiles.isEmpty()) {
            return true;
        }
        
        // Get the first existing media file to check device
        for (FeedMedia media : mediaFiles) {
            if (media.getLocalFileUrl() != null) {
                File oldFile = new File(media.getLocalFileUrl());
                if (oldFile.exists()) {
                    File newDir = new File(newBasePath);
                    try {
                        // Compare canonical paths to determine if on same filesystem
                        String oldRoot = oldFile.getCanonicalFile().getAbsolutePath();
                        String newRoot = newDir.getCanonicalFile().getAbsolutePath();
                        
                        // Simple heuristic: if both paths start with same root (e.g., /storage/emulated/0)
                        // they're likely on the same device
                        return oldRoot.substring(0, Math.min(oldRoot.length(), 20))
                                .equals(newRoot.substring(0, Math.min(newRoot.length(), 20)));
                    } catch (IOException e) {
                        Log.w(TAG, "Could not determine if same device, assuming different", e);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasEnoughSpace(List<FeedMedia> mediaFiles, String newBasePath) {
        File targetDir = new File(newBasePath);
        
        // Ensure target directory exists and is writable
        if (!isWritable(targetDir)) {
            Log.e(TAG, "Target directory not writable: " + newBasePath);
            return false;
        }
        
        long availableSpace;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                android.os.storage.StorageManager storageManager = (android.os.storage.StorageManager) 
                        getApplicationContext().getSystemService(Context.STORAGE_SERVICE);
                java.util.UUID uuid = storageManager.getUuidForPath(targetDir);
                availableSpace = storageManager.getAllocatableBytes(uuid);
            } catch (Exception e) {
                availableSpace = targetDir.getUsableSpace();
            }
        } else {
            availableSpace = targetDir.getUsableSpace();
        }
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
    
    private boolean isWritable(File dir) {
        return dir != null && dir.exists() && dir.canRead() && dir.canWrite();
    }
    
    private boolean moveMediaFile(FeedMedia media, String newBasePath) {
        if (media.getLocalFileUrl() == null) {
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
            if (!newFile.getParentFile().mkdirs() && !newFile.getParentFile().exists()) {
                Log.e(TAG, "Could not create parent directories for: " + newFile.getAbsolutePath());
                return false;
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
            if (newFile.exists()) {
                if (!newFile.delete()) {
                    Log.w(TAG, "Could not delete temporary file: " + newFile.getAbsolutePath());
                }
            }
            
            return false;
        }
    }

    /**
     * Updates the download state of media if the file exists but the episode is not marked as downloaded.
     * This handles cases where files were moved/restored but the database state wasn't updated.
     * 
     * @param media The FeedMedia object to check and potentially update
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
                Log.w(TAG, "Could not delete old transcript file: " + oldTranscript.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to move transcript file", e);
            // Don't fail the whole operation for transcript files
        }
    }

    private String generateNewPath(FeedMedia media, String newBasePath) {
        String feedTitle = FileNameGenerator.generateFileName(
                media.getItem().getFeed().getTitle());
        String mediaPath = "media/" + feedTitle;
        String fileName = new File(media.getLocalFileUrl()).getName();

        return new File(new File(newBasePath, mediaPath), fileName).getAbsolutePath();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager)
                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Media Migration",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Shows progress when moving media files");
            manager.createNotificationChannel(channel);
        }
    }
    
    private ForegroundInfo createForegroundInfo(String message) {
        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Moving media files")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(NOTIFICATION_ID, builder.build(), 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            return new ForegroundInfo(NOTIFICATION_ID, builder.build());
        }
    }
    
    private void showCompletionNotification(boolean success, FailureReason failureReason) {
        createNotificationChannel();

        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Cancel the foreground notification
        manager.cancel(NOTIFICATION_ID);
        
        String title = success ? "Migration completed" : "Migration failed";
        String message = success ? "Media files moved successfully" : getFailureMessage(failureReason);
        int icon = success ? android.R.drawable.stat_sys_download_done : android.R.drawable.stat_notify_error;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
            
        manager.notify(NOTIFICATION_ID + 1, builder.build());
    }
    
    private String getFailureMessage(FailureReason reason) {
        if (reason == null) {
            return "Could not complete media file migration";
        }
        switch (reason) {
            case NOT_ENOUGH_SPACE:
                return "Not enough space";
            case FAILED_TO_MOVE_FILES:
                return "Failed to move files";
            case UNEXPECTED_ERROR:
                return "Unexpected error occurred";
            case PARTIAL_SUCCESS:
                return "Some files moved successfully";
            default:
                return "Could not complete media file migration";
        }
    }

    private void showPartialSuccessNotification(int total, int failedCount) {
        int successCount = total - failedCount;
        int successPercentage = (int) ((successCount * 100.0) / total);

        NotificationManager notificationManager
                = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Files moved successfully")
                .setContentText(successPercentage + "% of files moved (" + successCount + "/" + total + ")")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        
        notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
    }

    private void copyFile(File source, File dest) throws IOException {
        try (
                FileInputStream in = new FileInputStream(source);
                FileOutputStream out = new FileOutputStream(dest)
        ) {
            byte[] buffer = new byte[65536]; // 64KB buffer for better performance
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
