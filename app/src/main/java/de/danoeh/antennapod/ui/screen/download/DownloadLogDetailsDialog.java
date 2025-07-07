package de.danoeh.antennapod.ui.screen.download;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.ui.appstartintent.OnlineFeedviewActivityStarter;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import org.greenrobot.eventbus.EventBus;

public class DownloadLogDetailsDialog extends MaterialAlertDialogBuilder {

    private Runnable dialogDismissFunction;

    /**
     * Creates a dialog with Feed title (and FeedItem title if possible).
     */
    public DownloadLogDetailsDialog(@NonNull Activity activity, DownloadResult status) {
        this(activity, status, false, null);
    }

    /**
     * Creates a dialog with Feed title (and FeedItem title if possible).
     * The title(s) can be made clickable, so they jump to the Feed if clicked.
     * A callback can be set that will be called when the dialog is closed.
     */
    public DownloadLogDetailsDialog(@NonNull Activity activity, DownloadResult status, boolean isTitleClickable, @Nullable Runnable onDismissCallback) {
        super(activity);

        Feed feed = null;
        String podcastTitle = null;
        String episodeTitle = null;

        String url = "unknown";
        if (status.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            FeedMedia media = DBReader.getFeedMedia(status.getFeedfileId());
            feed = DBReader.getFeed(media.getItem().getFeedId(), false, 0, 0);
            if (media != null) {
                podcastTitle = feed.getFeedTitle();
                episodeTitle = media.getEpisodeTitle();
                url = media.getDownloadUrl();
            }
        } else if (status.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            feed = DBReader.getFeed(status.getFeedfileId(), false, 0, 0);
            if (feed != null) {
                podcastTitle = feed.getFeedTitle();
                url = feed.getDownloadUrl();
            }
        }

        String message = activity.getString(R.string.download_successful);
        if (!status.isSuccessful()) {
            message = status.getReasonDetailed();
        }

        String humanReadableReason = activity.getString(DownloadErrorLabel.from(status.getReason()));
        SpannableString errorMessage = new SpannableString(activity.getString(R.string.download_log_details_message,
                humanReadableReason, message, url));
        errorMessage.setSpan(new ForegroundColorSpan(0x88888888),
                humanReadableReason.length(), errorMessage.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        CharSequence downloadMessage = getDownloadMessage(podcastTitle, episodeTitle);

        SpannableString clickableDownloadMessage;
        if (isTitleClickable) {
            clickableDownloadMessage = getClickableMessage(activity, downloadMessage, feed, onDismissCallback);
        } else {
            clickableDownloadMessage = new SpannableString(downloadMessage);
        }

        setTitle(R.string.download_error_details);
        setMessage(TextUtils.concat(clickableDownloadMessage, errorMessage));
        setPositiveButton(android.R.string.ok, null);
        setNeutralButton(R.string.copy_to_clipboard, (copyDialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(activity.getString(R.string.download_error_details), errorMessage);
            clipboard.setPrimaryClip(clip);
            if (Build.VERSION.SDK_INT < 32) {
                EventBus.getDefault().post(new MessageEvent(activity.getString(R.string.copied_to_clipboard)));
            }
        });
    }

    @NonNull
    private SpannableString getClickableMessage(@NonNull Activity activity, CharSequence downloadMessage, Feed feed, Runnable onDismissCallback) {
        SpannableString clickableDownloadMessage = new SpannableString(downloadMessage);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if (feed != null) {
                    if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                        Fragment fragment = FeedItemlistFragment.newInstance(feed.getId());
                        ((MainActivity) activity).loadChildFragment(fragment);
                    } else {
                        Intent intent = new OnlineFeedviewActivityStarter(getContext(), feed.getDownloadUrl())
                                .getIntent();
                        activity.startActivity(intent);
                    }
                }
                // Close the dialog.
                dialogDismissFunction.run();
                // Notify the outside that the dialog was dismissed, if wanted.
                if (onDismissCallback != null) {
                    onDismissCallback.run();
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                // Set color to theme based text color. This works for API Version 21 as well.
                //                android.util.TypedValue typedValue = new android.util.TypedValue();
                //                activity.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                //                ds.setColor(androidx.core.content.res.ResourcesCompat.getColor(
                //                        activity.getResources(), typedValue.resourceId, activity.getTheme()));
            }
        };
        clickableDownloadMessage.setSpan(clickableSpan, 0, clickableDownloadMessage.length(), 0);
        return clickableDownloadMessage;
    }

    @NonNull
    private static CharSequence getDownloadMessage(String podcastTitle, String episodeTitle) {
        CharSequence downloadMessage = "";
        if (podcastTitle != null) {
            downloadMessage += podcastTitle + "\n";
        }
        if (episodeTitle != null) {
            downloadMessage += episodeTitle + "\n";
        }
        if (!TextUtils.isEmpty(downloadMessage)) {
            downloadMessage += "\n";
        }
        return downloadMessage;
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setTextIsSelectable(true);
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        dialogDismissFunction = dialog::dismiss;
        return dialog;
    }

//    private void openFragment(Activity activity, Feed feed) {
//        if (feed == null) {
//            return;
//        }
//        if (feed.getState() == Feed.STATE_SUBSCRIBED) {
//            Fragment fragment = FeedItemlistFragment.newInstance(feed.getId());
//            ((MainActivity) activity).loadChildFragment(fragment);
//        } else {
//            Intent intent= new OnlineFeedviewActivityStarter(getContext(), feed.getDownloadUrl())
//                    .getIntent();
//            activity.startActivity(intent);
//        }
//    }
//
//    /**
//     * Switches to the Fragment if it isn't open yet.
//     * Was used to switch from the FeedItemListFragment error TextField to the dialog and back
//     * (when clicking the episode title link).
//     */
//    private void openFragmentConditionally(Activity activity, Feed feed) {
//        if (feed == null) {
//            return;
//        }
//        if (feed.getState() == Feed.STATE_SUBSCRIBED) {
//            MainActivity mainActivity = (MainActivity) activity;
//            FragmentManager fm = mainActivity.getSupportFragmentManager();
//            Fragment current = fm.findFragmentById(R.id.main_content_view);
//
//            if (current instanceof FeedItemlistFragment) {
//                return;
//            }
//
//            Fragment fragment = FeedItemlistFragment.newInstance(feed.getId());
//            ((MainActivity) activity).loadChildFragment(fragment);
////            fm.beginTransaction()
////                    .replace(R.id.main_content_view, fragment)
////                    .commit();
//        } else {
//            Intent intent= new OnlineFeedviewActivityStarter(getContext(), feed.getDownloadUrl())
//                    .getIntent();
//            activity.startActivity(intent);
//        }
//    }
}