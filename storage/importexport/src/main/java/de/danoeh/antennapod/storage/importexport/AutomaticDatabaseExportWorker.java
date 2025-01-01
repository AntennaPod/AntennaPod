package de.danoeh.antennapod.storage.importexport;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.greenrobot.eventbus.EventBus;

public class AutomaticDatabaseExportWorker extends Worker {
    private static final String WORK_ID_AUTOMATIC_DATABASE_EXPORT = "de.danoeh.antennapod.AutomaticDbExport";

    public static void enqueueIfNeeded(Context context, boolean replace) {
        if (UserPreferences.getAutomaticExportFolder() == null) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_ID_AUTOMATIC_DATABASE_EXPORT);
        } else {
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                        AutomaticDatabaseExportWorker.class, 3, TimeUnit.DAYS)
                    .build();
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_ID_AUTOMATIC_DATABASE_EXPORT,
                    replace ? ExistingPeriodicWorkPolicy.REPLACE : ExistingPeriodicWorkPolicy.KEEP, workRequest);
        }
    }

    public AutomaticDatabaseExportWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @NonNull
    public Result doWork() {
        String folderUri = UserPreferences.getAutomaticExportFolder();
        if (folderUri == null) {
            return Result.success();
        }
        try {
            export(folderUri);
            return Result.success();
        } catch (IOException e) {
            showErrorNotification(e);
            return Result.failure();
        }
    }

    private void export(String folderUri) throws IOException {
        DocumentFile documentFolder = DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(folderUri));
        if (documentFolder == null || !documentFolder.exists() || !documentFolder.canWrite()) {
            throw new IOException("Unable to open export folder");
        }
        String filename = String.format("AntennaPodBackup-%s.db",
                new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        DocumentFile exportFile = documentFolder.createFile("application/x-sqlite3", filename);
        if (exportFile == null || !exportFile.canWrite()) {
            throw new IOException("Unable to create export file");
        }
        DatabaseExporter.exportToDocument(exportFile.getUri(), getApplicationContext());
        List<DocumentFile> files = new ArrayList<>(Arrays.asList(documentFolder.listFiles()));
        Iterator<DocumentFile> itr = files.iterator();
        while (itr.hasNext()) {
            DocumentFile file = itr.next();
            if (!file.getName().matches("AntennaPodBackup-\\d\\d\\d\\d-\\d\\d-\\d\\d\\.db")) {
                itr.remove();
            }
        }
        Collections.sort(files, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
        for (int i = 5; i < files.size(); i++) {
            files.get(i).delete();
        }
    }

    private void showErrorNotification(Exception exception) {
        final String description = getApplicationContext().getString(R.string.automatic_database_export_error)
                + " " + exception.getMessage();
        if (EventBus.getDefault().hasSubscriberForEvent(MessageEvent.class)) {
            EventBus.getDefault().post(new MessageEvent(description));
            return;
        }

        Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(
                getApplicationContext().getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                R.id.pending_intent_backup_error, intent, PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                        NotificationUtils.CHANNEL_ID_SYNC_ERROR)
                .setContentTitle(getApplicationContext().getString(R.string.automatic_database_export_error))
                .setContentText(exception.getMessage())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_sync_error)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            nm.notify(R.id.notification_id_backup_error, notification);
        } else {
            Toast.makeText(getApplicationContext(), description, Toast.LENGTH_LONG).show();
        }
    }
}
