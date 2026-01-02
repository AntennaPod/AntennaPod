package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.DialogInterface;
import android.util.Log;

import androidx.annotation.PluralsRes;
import androidx.fragment.app.FragmentActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.databinding.PlaybackSpeedFeedSettingDialogBinding;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.screen.feed.RemoveFeedDialog;
import de.danoeh.antennapod.ui.screen.feed.preferences.TagSettingsDialog;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceListDialog;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceSwitchDialog;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.ui.share.ShareUtils;

public class FeedMultiSelectActionHandler {
    private static final String TAG = "FeedSelectHandler";
    private final FragmentActivity activity;
    private final List<Feed> selectedItems;

    public FeedMultiSelectActionHandler(FragmentActivity activity, List<Feed> selectedItems) {
        this.activity = activity;
        this.selectedItems = selectedItems;
    }

    public void handleAction(int id) {
        if (selectedItems.isEmpty()) {
            return;
        }
        if (id == R.id.remove_archive_feed || id == R.id.remove_restore_feed) {
            new RemoveFeedDialog(selectedItems).show(activity.getSupportFragmentManager(), null);
        } else if (id == R.id.notify_new_episodes) {
            notifyNewEpisodesPrefHandler();
        } else if (id == R.id.keep_updated) {
            keepUpdatedPrefHandler();
        } else if (id == R.id.autodownload) {
            autoDownloadPrefHandler();
        } else if (id == R.id.autoDeleteDownload) {
            autoDeleteEpisodesPrefHandler();
        } else if (id == R.id.playback_speed) {
            playbackSpeedPrefHandler();
        } else if (id == R.id.edit_tags) {
            editFeedPrefTags();
        } else if (id == R.id.remove_all_inbox_item) {
            removeAllFromInbox();
        } else if (id == R.id.share_feed) {
            if (!selectedItems.get(0).isLocalFeed()) {
                ShareUtils.shareFeedLink(activity, selectedItems.get(0));
            }
        } else {
            Log.e(TAG, "Unrecognized speed dial action item. Do nothing. id=" + id);
        }
    }

    private void notifyNewEpisodesPrefHandler() {
        PreferenceSwitchDialog preferenceSwitchDialog = new PreferenceSwitchDialog(activity,
                activity.getString(R.string.episode_notification),
                activity.getString(R.string.episode_notification_summary));
        preferenceSwitchDialog.setOnPreferenceChangedListener(enabled ->
                saveFeedPreferences(feedPreferences -> feedPreferences.setShowEpisodeNotification(enabled)));
        preferenceSwitchDialog.openDialog();
    }

    private void autoDownloadPrefHandler() {
        PreferenceListDialog preferenceListDialog = new PreferenceListDialog(activity,
                activity.getString(R.string.auto_download_label));
        String[] items = activity.getResources().getStringArray(R.array.spnEnableAutoDownloadItems);
        preferenceListDialog.openDialog(items);
        preferenceListDialog.setOnPreferenceChangedListener(which -> {
            FeedPreferences.AutoDownloadSetting autoDownloadSetting = switch (which) {
                case 1 -> FeedPreferences.AutoDownloadSetting.ENABLED;
                case 2 -> FeedPreferences.AutoDownloadSetting.DISABLED;
                default -> FeedPreferences.AutoDownloadSetting.GLOBAL;
            };
            saveFeedPreferences(feedPreferences -> feedPreferences.setAutoDownload(autoDownloadSetting));
        });
    }

    private void playbackSpeedPrefHandler() {
        PlaybackSpeedFeedSettingDialogBinding viewBinding =
                PlaybackSpeedFeedSettingDialogBinding.inflate(activity.getLayoutInflater());
        viewBinding.seekBar.setProgressChangedListener(speed ->
                viewBinding.currentSpeedLabel.setText(String.format(Locale.getDefault(), "%.2fx", speed)));
        viewBinding.useGlobalCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewBinding.seekBar.setEnabled(!isChecked);
            viewBinding.seekBar.setAlpha(isChecked ? 0.4f : 1f);
            viewBinding.currentSpeedLabel.setAlpha(isChecked ? 0.4f : 1f);
        });
        viewBinding.seekBar.updateSpeed(1.0f);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.playback_speed)
                .setView(viewBinding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    float newSpeed = viewBinding.useGlobalCheckbox.isChecked()
                            ? FeedPreferences.SPEED_USE_GLOBAL : viewBinding.seekBar.getCurrentSpeed();
                    saveFeedPreferences(feedPreferences -> feedPreferences.setFeedPlaybackSpeed(newSpeed));
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private void autoDeleteEpisodesPrefHandler() {
        PreferenceListDialog preferenceListDialog = new PreferenceListDialog(activity,
                activity.getString(R.string.pref_auto_delete_playback_title));
        String[] items = activity.getResources().getStringArray(R.array.spnAutoDeleteItems);
        preferenceListDialog.openDialog(items);
        preferenceListDialog.setOnPreferenceChangedListener(which -> {
            FeedPreferences.AutoDeleteAction autoDeleteAction = FeedPreferences.AutoDeleteAction.fromCode(which);
            saveFeedPreferences(feedPreferences -> feedPreferences.setAutoDeleteAction(autoDeleteAction));
        });
    }

    private void keepUpdatedPrefHandler() {
        PreferenceSwitchDialog preferenceSwitchDialog = new PreferenceSwitchDialog(activity,
                activity.getString(R.string.kept_updated),
                activity.getString(R.string.keep_updated_summary));
        preferenceSwitchDialog.setOnPreferenceChangedListener(keepUpdated ->
                saveFeedPreferences(feedPreferences -> feedPreferences.setKeepUpdated(keepUpdated)));
        preferenceSwitchDialog.openDialog();
    }

    private void showMessage(@PluralsRes int msgId, int numItems) {
        EventBus.getDefault().post(new MessageEvent(activity.getResources()
                .getQuantityString(msgId, numItems, numItems)));
    }

    private void saveFeedPreferences(Consumer<FeedPreferences> preferencesConsumer) {
        for (Feed feed : selectedItems) {
            preferencesConsumer.accept(feed.getPreferences());
            DBWriter.setFeedPreferences(feed.getPreferences());
        }
        showMessage(R.plurals.updated_feeds_batch_label, selectedItems.size());
    }

    private void editFeedPrefTags() {
        ArrayList<FeedPreferences> preferencesList = new ArrayList<>();
        for (Feed feed : selectedItems) {
            preferencesList.add(feed.getPreferences());
        }
        TagSettingsDialog.newInstance(preferencesList).show(activity.getSupportFragmentManager(),
                TagSettingsDialog.TAG);
    }

    private void removeAllFromInbox() {
        new ConfirmationDialog(activity, R.string.remove_all_inbox_label, R.string.remove_all_inbox_confirmation_msg) {
            @Override
            public void onConfirmButtonPressed(DialogInterface clickedDialog) {
                clickedDialog.dismiss();
                Observable.fromAction(() -> {
                    for (Feed selectedFeed : selectedItems) {
                        DBWriter.removeFeedNewFlag(selectedFeed.getId());
                    }
                })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> { }, error -> Log.e(TAG, Log.getStackTraceString(error)));
            }
        }.createNewDialog().show();
    }
}
