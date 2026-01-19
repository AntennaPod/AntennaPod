package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.PlaybackSpeedFeedSettingDialogBinding;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.settings.SkipIntroEndingChangedEvent;
import de.danoeh.antennapod.event.settings.SpeedPresetChangedEvent;
import de.danoeh.antennapod.event.settings.VolumeAdaptionChangedEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedFilter;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.preferences.screen.synchronization.AuthenticationDialog;
import de.danoeh.antennapod.ui.screen.feed.RenameFeedDialog;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeOnSubscribe;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FeedSettingsPreferenceFragment extends PreferenceFragmentCompat {
    private static final String TAG = "FeedSettingsPrefFrag";
    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String PREF_EPISODE_FILTER = "episodeFilter";
    private static final String PREF_AUTODOWNLOAD = "includeAutoDownload";
    private static final String PREF_SCREEN = "feedSettingsScreen";
    private static final String PREF_AUTHENTICATION = "authentication";
    private static final String PREF_AUTO_DELETE = "autoDelete";
    private static final String PREF_NEW_EPISODES_ACTION = "feedNewEpisodesAction";
    private static final String PREF_FEED_PLAYBACK_SPEED = "feedPlaybackSpeed";
    private static final String PREF_AUTO_SKIP = "feedAutoSkip";
    private static final String PREF_NOTIFICATION = "episodeNotification";
    private static final String PREF_RENAME = "rename";
    private static final String PREF_TAGS = "tags";

    private Feed feed;
    private Disposable disposable;
    private FeedPreferences feedPreferences;

    public static FeedSettingsPreferenceFragment newInstance(long feedId) {
        FeedSettingsPreferenceFragment fragment = new FeedSettingsPreferenceFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_FEED_ID, feedId);
        fragment.setArguments(arguments);
        return fragment;
    }

    boolean notificationPermissionDenied = false;
    private final ActivityResultLauncher<String> enableNotificationsRequestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    SwitchPreferenceCompat pref = findPreference(PREF_NOTIFICATION);
                    pref.setChecked(true);
                    pref.callChangeListener(true);
                    return;
                }
                if (notificationPermissionDenied) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    return;
                }
                Toast.makeText(getContext(), R.string.notification_permission_denied, Toast.LENGTH_LONG).show();
                notificationPermissionDenied = true;
            });

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        final RecyclerView view = super.onCreateRecyclerView(inflater, parent, state);
        // To prevent transition animation because of summary update
        view.setItemAnimator(null);
        view.setLayoutAnimation(null);
        return view;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.feed_settings);
        // To prevent displaying partially loaded data
        findPreference(PREF_SCREEN).setVisible(false);

        long feedId = getArguments().getLong(EXTRA_FEED_ID);
        disposable = Maybe.create((MaybeOnSubscribe<Feed>) emitter -> {
            Feed feed = DBReader.getFeed(feedId, false, 0, 0);
            if (feed != null) {
                emitter.onSuccess(feed);
            } else {
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    feed = result;
                    feedPreferences = feed.getPreferences();

                    setupPreferences();
                    updateAutoDeleteSummary();
                    updateAutoDownloadEnabledSummary();
                    updateNewEpisodesActionSummary();

                    if (feed.isLocalFeed()) {
                        findPreference(PREF_AUTHENTICATION).setVisible(false);
                        findPreference(PREF_AUTODOWNLOAD).setVisible(false);
                        findPreference(PREF_EPISODE_FILTER).setVisible(false);
                    }

                    findPreference(PREF_SCREEN).setVisible(true);
                }, error -> Log.d(TAG, Log.getStackTraceString(error)), () -> { });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void setupPreferences() {
        findPreference(PREF_AUTO_SKIP).setOnPreferenceClickListener(preference -> {
            new FeedPreferenceSkipDialog(getContext(),
                    feedPreferences.getFeedSkipIntro(), feedPreferences.getFeedSkipEnding()) {
                @Override
                protected void onConfirmed(int skipIntro, int skipEnding) {
                    feedPreferences.setFeedSkipIntro(skipIntro);
                    feedPreferences.setFeedSkipEnding(skipEnding);
                    DBWriter.setFeedPreferences(feedPreferences);
                    EventBus.getDefault().post(
                            new SkipIntroEndingChangedEvent(feedPreferences.getFeedSkipIntro(),
                                    feedPreferences.getFeedSkipEnding(), feed.getId()));
                }
            }.show();
            return false;
        });
        findPreference(PREF_FEED_PLAYBACK_SPEED).setOnPreferenceClickListener(this::showPlaybackSpeedDialog);
        findPreference(PREF_EPISODE_FILTER).setOnPreferenceClickListener(preference -> {
            new EpisodeFilterDialog(getContext(), feedPreferences.getFilter()) {
                @Override
                protected void onConfirmed(FeedFilter filter) {
                    feedPreferences.setFilter(filter);
                    DBWriter.setFeedPreferences(feedPreferences);
                }
            }.show();
            return false;
        });
        findPreference(PREF_AUTHENTICATION).setOnPreferenceClickListener(preference -> {
            new AuthenticationDialog(getContext(),
                    R.string.authentication_label, true,
                    feedPreferences.getUsername(), feedPreferences.getPassword()) {
                @Override
                protected void onConfirmed(String username, String password) {
                    feedPreferences.setUsername(username);
                    feedPreferences.setPassword(password);
                    Future<?> setPreferencesFuture = DBWriter.setFeedPreferences(feedPreferences);

                    new Thread(() -> {
                        try {
                            setPreferencesFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        FeedUpdateManager.getInstance().runOnce(getContext(), feed);
                    }, "RefreshAfterCredentialChange").start();
                }
            }.show();
            return false;
        });
        findPreference(PREF_AUTO_DELETE).setOnPreferenceChangeListener((preference, newValue) -> {
            feedPreferences.setAutoDeleteAction(
                    FeedPreferences.AutoDeleteAction.fromCode(Integer.parseInt((String) newValue)));
            DBWriter.setFeedPreferences(feedPreferences);
            updateAutoDeleteSummary();
            return false;
        });
        ListPreference volumeAdaptationPreference = findPreference("volumeReduction");
        volumeAdaptationPreference.setValue("" + feedPreferences.getVolumeAdaptionSetting().toInteger());
        volumeAdaptationPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            VolumeAdaptionSetting newSetting = VolumeAdaptionSetting.fromInteger(Integer.parseInt((String) newValue));
            feedPreferences.setVolumeAdaptionSetting(newSetting);
            DBWriter.setFeedPreferences(feedPreferences);
            volumeAdaptationPreference.setValue("" + feedPreferences.getVolumeAdaptionSetting().toInteger());
            EventBus.getDefault().post(new VolumeAdaptionChangedEvent(newSetting, feed.getId()));
            return false;
        });
        findPreference(PREF_NEW_EPISODES_ACTION).setOnPreferenceClickListener(preference -> {
            boolean isAutoDownload = feed.getPreferences().isAutoDownload(UserPreferences.isEnableAutodownloadGlobal());
            if (isAutoDownload && !feed.isLocalFeed()) {
                EventBus.getDefault().post(new MessageEvent(getString(R.string.feed_new_episodes_action_snackbar)));
                return true;
            }
            return false;
        });
        findPreference(PREF_NEW_EPISODES_ACTION).setOnPreferenceChangeListener((preference, newValue) -> {
            int code = Integer.parseInt((String) newValue);
            feedPreferences.setNewEpisodesAction(FeedPreferences.NewEpisodesAction.fromCode(code));
            DBWriter.setFeedPreferences(feedPreferences);
            updateNewEpisodesActionSummary();
            return false;
        });
        SwitchPreferenceCompat keepUpdated = findPreference("keepUpdated");
        keepUpdated.setChecked(feedPreferences.getKeepUpdated());
        keepUpdated.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean checked = Boolean.TRUE.equals(newValue);
            feedPreferences.setKeepUpdated(checked);
            DBWriter.setFeedPreferences(feedPreferences);
            keepUpdated.setChecked(checked);
            return false;
        });
        findPreference(PREF_AUTODOWNLOAD).setOnPreferenceChangeListener((preference, newValue) -> {
            feedPreferences.setAutoDownload(
                    FeedPreferences.AutoDownloadSetting.fromInteger(Integer.parseInt((String) newValue)));
            DBWriter.setFeedPreferences(feedPreferences);
            updateAutoDownloadEnabledSummary();
            updateNewEpisodesActionSummary();
            return false;
        });
        findPreference(PREF_TAGS).setOnPreferenceClickListener(preference -> {
            TagSettingsDialog.newInstance(Collections.singletonList(feedPreferences))
                    .show(getChildFragmentManager(), TagSettingsDialog.TAG);
            return true;
        });
        SwitchPreferenceCompat notificationPreference = findPreference(PREF_NOTIFICATION);
        notificationPreference.setChecked(feedPreferences.getShowEpisodeNotification());
        notificationPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean checked = Boolean.TRUE.equals(newValue);
            if (checked && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                enableNotificationsRequestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return false;
            }
            feedPreferences.setShowEpisodeNotification(checked);
            DBWriter.setFeedPreferences(feedPreferences);
            notificationPreference.setChecked(checked);
            return false;
        });
        findPreference(PREF_RENAME).setOnPreferenceClickListener(preference -> {
            new RenameFeedDialog(getActivity(), feed).show();
            return true;
        });
    }

    private void updateAutoDeleteSummary() {
        ListPreference autoDeletePreference = findPreference(PREF_AUTO_DELETE);
        boolean isEnabledGlobally = feed.isLocalFeed()
                ? UserPreferences.isAutoDeleteLocal() : UserPreferences.isAutoDelete();
        int globalStringResource = isEnabledGlobally
                ? R.string.feed_auto_download_always : R.string.feed_auto_download_never;
        String summary = switch (feedPreferences.getAutoDeleteAction()) {
            case GLOBAL -> getString(R.string.global_default_with_value, getString(globalStringResource));
            case ALWAYS -> getString(R.string.feed_auto_download_always);
            default -> getString(R.string.feed_auto_download_never);
        };
        autoDeletePreference.setSummary(summary);
        autoDeletePreference.setValue("" + feedPreferences.getAutoDeleteAction().code);
    }

    private void updateNewEpisodesActionSummary() {
        if (feed == null || feed.getPreferences() == null) {
            return;
        }
        ListPreference newEpisodesAction = findPreference(PREF_NEW_EPISODES_ACTION);
        boolean isAutoDownload = feed.getPreferences().isAutoDownload(UserPreferences.isEnableAutodownloadGlobal());
        if (isAutoDownload && !feed.isLocalFeed()) {
            newEpisodesAction.setSummary(R.string.feed_new_episodes_action_summary_autodownload);
            return;
        }
        newEpisodesAction.setEnabled(true);
        newEpisodesAction.setValue("" + feedPreferences.getNewEpisodesAction().code);
        int globalStringResource = switch (UserPreferences.getNewEpisodesAction()) {
            case ADD_TO_INBOX -> R.string.feed_new_episodes_action_add_to_inbox;
            case ADD_TO_QUEUE -> R.string.feed_new_episodes_action_add_to_queue;
            default -> R.string.feed_new_episodes_action_nothing;
        };
        String summary = switch (feedPreferences.getNewEpisodesAction()) {
            case GLOBAL -> getString(R.string.global_default_with_value, getString(globalStringResource));
            case ADD_TO_INBOX -> getString(R.string.feed_new_episodes_action_add_to_inbox);
            case ADD_TO_QUEUE -> getString(R.string.feed_new_episodes_action_add_to_queue);
            default -> getString(R.string.feed_new_episodes_action_nothing);
        };
        newEpisodesAction.setSummary(summary);
    }

    private void updateAutoDownloadEnabledSummary() {
        if (feed == null || feed.getPreferences() == null) {
            return;
        }
        boolean enabled = feed.getPreferences().isAutoDownload(UserPreferences.isEnableAutodownloadGlobal());
        findPreference(PREF_EPISODE_FILTER).setVisible(enabled);
        ListPreference autoDownloadPreference = findPreference(PREF_AUTODOWNLOAD);
        String summary = switch (feedPreferences.getAutoDownload()) {
            case GLOBAL -> getString(R.string.global_default_with_value,
                    getString(enabled ? R.string.enabled : R.string.disabled));
            case ENABLED -> getString(R.string.enabled);
            case DISABLED -> getString(R.string.disabled);
        };
        autoDownloadPreference.setSummary(summary);
        autoDownloadPreference.setValue("" + feedPreferences.getAutoDownload().code);
    }

    private boolean showPlaybackSpeedDialog(Preference preference) {
        PlaybackSpeedFeedSettingDialogBinding viewBinding =
                PlaybackSpeedFeedSettingDialogBinding.inflate(getLayoutInflater());
        viewBinding.seekBar.setProgressChangedListener(speed ->
                viewBinding.currentSpeedLabel.setText(String.format(Locale.getDefault(), "%.2fx", speed)));
        viewBinding.useGlobalCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewBinding.seekBar.setEnabled(!isChecked);
            viewBinding.seekBar.setAlpha(isChecked ? 0.4f : 1f);
            viewBinding.currentSpeedLabel.setAlpha(isChecked ? 0.4f : 1f);

            viewBinding.skipSilenceFeed.setEnabled(!isChecked);
            viewBinding.skipSilenceFeed.setAlpha(isChecked ? 0.4f : 1f);
        });
        float speed = feedPreferences.getFeedPlaybackSpeed();
        FeedPreferences.SkipSilence skipSilence = feedPreferences.getFeedSkipSilence();
        boolean isGlobal = speed == FeedPreferences.SPEED_USE_GLOBAL;
        viewBinding.useGlobalCheckbox.setChecked(isGlobal);
        viewBinding.seekBar.updateSpeed(isGlobal ? 1 : speed);
        viewBinding.skipSilenceFeed.setChecked(!isGlobal
                && skipSilence == FeedPreferences.SkipSilence.AGGRESSIVE);
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.playback_speed)
                .setView(viewBinding.getRoot())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    float newSpeed = viewBinding.useGlobalCheckbox.isChecked()
                            ? FeedPreferences.SPEED_USE_GLOBAL : viewBinding.seekBar.getCurrentSpeed();
                    feedPreferences.setFeedPlaybackSpeed(newSpeed);
                    FeedPreferences.SkipSilence newSkipSilence;
                    if (viewBinding.useGlobalCheckbox.isChecked()) {
                        newSkipSilence = FeedPreferences.SkipSilence.GLOBAL;
                    } else if (viewBinding.skipSilenceFeed.isChecked()) {
                        newSkipSilence = FeedPreferences.SkipSilence.AGGRESSIVE;
                    } else {
                        newSkipSilence = FeedPreferences.SkipSilence.OFF;
                    }
                    feedPreferences.setFeedSkipSilence(newSkipSilence);
                    DBWriter.setFeedPreferences(feedPreferences);
                    EventBus.getDefault().post(new SpeedPresetChangedEvent(feedPreferences.getFeedPlaybackSpeed(),
                            feed.getId(), feedPreferences.getFeedSkipSilence()));
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
        return true;
    }
}