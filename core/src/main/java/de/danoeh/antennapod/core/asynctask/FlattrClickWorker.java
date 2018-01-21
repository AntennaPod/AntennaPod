package de.danoeh.antennapod.core.asynctask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.shredzone.flattr4j.exception.FlattrException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.danoeh.antennapod.core.util.flattr.FlattrThing;
import de.danoeh.antennapod.core.util.flattr.FlattrUtils;

/**
 * Performs a click action in a background thread.
 * <p/>
 * When started, the flattr click worker will try to flattr every item that is in the flattr queue. If no network
 * connection is available it will shut down immediately. The FlattrClickWorker can also be given one additional
 * FlattrThing which will be flattrd immediately.
 * <p/>
 * The FlattrClickWorker will display a toast notification for every item that has been flattrd. If the FlattrClickWorker failed
 * to flattr something, a notification will be displayed.
 */
public class FlattrClickWorker extends AsyncTask<Void, Integer, FlattrClickWorker.ExitCode> {
    private static final String TAG = "FlattrClickWorker";

    private static final int NOTIFICATION_ID = 4;

    private final Context context;

    public enum ExitCode {EXIT_NORMAL, NO_TOKEN, NO_NETWORK, NO_THINGS}

    private final AtomicInteger countFailed = new AtomicInteger();
    private final AtomicInteger countSuccess = new AtomicInteger();

    private volatile FlattrThing extraFlattrThing;

    /**
     * Only relevant if just one thing is flattrd
     */
    private volatile FlattrException exception;

    /**
     * Creates a new FlattrClickWorker which will only flattr all things in the queue.
     * <p/>
     * The FlattrClickWorker has to be started by calling executeAsync().
     *
     * @param context A context for accessing the database and posting notifications. Must not be null.
     */
    public FlattrClickWorker(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Creates a new FlattrClickWorker which will flattr all things in the queue and one additional
     * FlattrThing.
     * <p/>
     * The FlattrClickWorker has to be started by calling executeAsync().
     *
     * @param context          A context for accessing the database and posting notifications. Must not be null.
     * @param extraFlattrThing The additional thing to flattr
     */
    public FlattrClickWorker(Context context, FlattrThing extraFlattrThing) {
        this(context);
        this.extraFlattrThing = extraFlattrThing;
    }


    @Override
    protected ExitCode doInBackground(Void... params) {

        if (!FlattrUtils.hasToken()) {
            return ExitCode.NO_TOKEN;
        }

        if (!NetworkUtils.networkAvailable()) {
            return ExitCode.NO_NETWORK;
        }

        final List<FlattrThing> flattrQueue = DBReader.getFlattrQueue();
        if (extraFlattrThing != null) {
            flattrQueue.add(extraFlattrThing);
        } else if (flattrQueue.size() == 1) {
            // if only one item is flattrd, the report can specifically mentioned that this item has failed
            extraFlattrThing = flattrQueue.get(0);
        }

        if (flattrQueue.isEmpty()) {
            return ExitCode.NO_THINGS;
        }

        List<Future<?>> dbFutures = new LinkedList<>();
        for (FlattrThing thing : flattrQueue) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Processing " + thing.getTitle());

            try {
                thing.getFlattrStatus().setUnflattred();  // pop from queue to prevent unflattrable things from getting stuck in flattr queue infinitely
                FlattrUtils.clickUrl(context, thing.getPaymentLink());
                thing.getFlattrStatus().setFlattred();
                publishProgress(R.string.flattr_click_success);
                countSuccess.incrementAndGet();

            } catch (FlattrException e) {
                e.printStackTrace();
                int failed = countFailed.incrementAndGet();
                if (failed == 1) {
                    exception = e;
                }
            }

            Future<?> f = DBWriter.setFlattredStatus(context, thing, false);
            if (f != null) {
                dbFutures.add(f);
            }
        }

        for (Future<?> f : dbFutures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return ExitCode.EXIT_NORMAL;
    }

    @Override
    protected void onPostExecute(ExitCode exitCode) {
        super.onPostExecute(exitCode);
        switch (exitCode) {
            case EXIT_NORMAL:
                if (countFailed.get() > 0) {
                    postFlattrFailedNotification();
                }
                break;
            case NO_NETWORK:
                postToastNotification(R.string.flattr_click_enqueued);
                break;
            case NO_TOKEN:
                postNoTokenNotification();
                break;
            case NO_THINGS: // nothing to notify here
                break;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        postToastNotification(values[0]);
    }

    private void postToastNotification(int msg) {
        Toast.makeText(context, context.getString(msg), Toast.LENGTH_LONG).show();
    }

    private void postNoTokenNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                ClientConfig.flattrCallbacks.getFlattrAuthenticationActivityIntent(context), 0);

        Notification notification = new NotificationCompat.Builder(context)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.no_flattr_token_notification_msg)))
                .setContentIntent(contentIntent)
                .setContentTitle(context.getString(R.string.no_flattr_token_title))
                .setTicker(context.getString(R.string.no_flattr_token_title))
                .setSmallIcon(R.drawable.stat_notify_sync_error)
                .setOngoing(false)
                .setAutoCancel(true)
                .build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
    }

    private void postFlattrFailedNotification() {
        int failed = countFailed.get();
        if (failed == 0) {
            return;
        }

        PendingIntent contentIntent = ClientConfig.flattrCallbacks.getFlattrFailedNotificationContentIntent(context);
        String title;
        String subtext;

        if (failed == 1) {
            title = context.getString(R.string.flattrd_failed_label);
            String exceptionMsg = (exception.getMessage() != null) ? exception.getMessage() : "";
            subtext = context.getString(R.string.flattr_click_failure, extraFlattrThing.getTitle())
                    + "\n" + exceptionMsg;
        } else {
            title = context.getString(R.string.flattrd_label);
            subtext = context.getString(R.string.flattr_click_success_count, countSuccess.get()) + "\n"
                    + context.getString(R.string.flattr_click_failure_count, failed);
        }

        Notification notification = new NotificationCompat.Builder(context)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(subtext))
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.drawable.stat_notify_sync_error)
                .setOngoing(false)
                .setAutoCancel(true)
                .build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification);
    }


    /**
     * Starts the FlattrClickWorker as an AsyncTask.
     */
    public void executeAsync() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
