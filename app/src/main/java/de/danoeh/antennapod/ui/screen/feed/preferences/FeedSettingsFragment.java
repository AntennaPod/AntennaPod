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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.settings.SkipIntroEndingChangedEvent;
import de.danoeh.antennapod.event.settings.SpeedPresetChangedEvent;
import de.danoeh.antennapod.event.settings.VolumeAdaptionChangedEvent;
import de.danoeh.antennapod.databinding.PlaybackSpeedFeedSettingDialogBinding;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedFilter;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.preferences.screen.synchronization.AuthenticationDialog;
import io.reactivex.Maybe;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FeedSettingsFragment extends Fragment {
    private static final String TAG = "FeedSettingsFragment";
    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";

    private Disposable disposable;

    public static FeedSettingsFragment newInstance(Feed feed) {
        FeedSettingsFragment fragment = new FeedSettingsFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_FEED_ID, feed.getId());
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.feedsettings, container, false);
        long feedId = getArguments().getLong(EXTRA_FEED_ID);

        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        getParentFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment_container,
                        FeedSettingsPreferenceFragment.newInstance(feedId), "settings_fragment")
                .commitAllowingStateLoss();

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
                .subscribe(result -> toolbar.setSubtitle(result.getTitle()),
                        error -> Log.d(TAG, Log.getStackTraceString(error)),
                        () -> { });


        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    public static class FeedSettingsPreferenceFragment extends PreferenceFragmentCompat {
        private static final String PREF_EPISODE_FILTER = "episodeFilter";
        private static final String PREF_SCREEN = "feedSettingsScreen";
        private static final String PREF_AUTHENTICATION = "authentication";
        private static final String PREF_AUTO_DELETE = "autoDelete";
        private static final String PREF_CATEGORY_AUTO_DOWNLOAD = "autoDownloadCategory";
        private static final String PREF_NEW_EPISODES_ACTION = "feedNewEpisodesAction";
        private static final String PREF_FEED_PLAYBACK_SPEED = "feedPlaybackSpeed";
        private static final String PREF_AUTO_SKIP = "feedAutoSkip";
        private static final String PREF_NOTIFICATION = "episodeNotification";
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

                        setupAutoDownloadGlobalPreference();
                        setupAutoDownloadPreference();
                        setupKeepUpdatedPreference();
                        setupAutoDeletePreference();
                        setupVolumeAdaptationPreferences();
                        setupNewEpisodesAction();
                        setupAuthentificationPreference();
                        setupEpisodeFilterPreference();
                        setupPlaybackSpeedPreference();
                        setupFeedAutoSkipPreference();
                        setupEpisodeNotificationPreference();
                        setupTags();

                        updateAutoDeleteSummary();
                        updateVolumeAdaptationValue();
                        updateAutoDownloadEnabled();
                        updateNewEpisodesAction();

                        if (feed.isLocalFeed()) {
                            findPreference(PREF_AUTHENTICATION).setVisible(false);
                            findPreference(PREF_CATEGORY_AUTO_DOWNLOAD).setVisible(false);
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

        private void setupFeedAutoSkipPreference() {
            findPreference(PREF_AUTO_SKIP).setOnPreferenceClickListener(preference -> {
                new FeedPreferenceSkipDialog(getContext(),
                        feedPreferences.getFeedSkipIntro(),
                        feedPreferences.getFeedSkipEnding()) {
                    @Override
                    protected void onConfirmed(int skipIntro, int skipEnding) {
                        feedPreferences.setFeedSkipIntro(skipIntro);
                        feedPreferences.setFeedSkipEnding(skipEnding);
                        DBWriter.setFeedPreferences(feedPreferences);
                        EventBus.getDefault().post(
                                new SkipIntroEndingChangedEvent(feedPreferences.getFeedSkipIntro(),
                                        feedPreferences.getFeedSkipEnding(),
                                        feed.getId()));
                    }
                }.show();
                return false;
            });
        }

        private void setupPlaybackSpeedPreference() {
            Preference feedPlaybackSpeedPreference = findPreference(PREF_FEED_PLAYBACK_SPEED);
            feedPlaybackSpeedPreference.setOnPreferenceClickListener(preference -> {
                PlaybackSpeedFeedSettingDialogBinding viewBinding =
                        PlaybackSpeedFeedSettingDialogBinding.inflate(getLayoutInflater());
                viewBinding.seekBar.setProgressChangedListener(speed ->
                        viewBinding.currentSpeedLabel.setText(String.format(Locale.getDefault(), "%.2fx", speed)));
                viewBinding.useGlobalCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    viewBinding.seekBar.setEnabled(!isChecked);
                    viewBinding.seekBar.setAlpha(isChecked ? 0.4f : 1f);
                    viewBinding.currentSpeedLabel.setAlpha(isChecked ? 0.4f : 1f);

                    viewBinding.skipSilenceSpinner.setEnabled(!isChecked);
                    viewBinding.skipSilenceSpinner.setAlpha(isChecked ? 0.4f : 1f);
                });
                float speed = feedPreferences.getFeedPlaybackSpeed();
                FeedPreferences.SkipSilence skipSilence = feedPreferences.getFeedSkipSilence();
                boolean isGlobal = speed == FeedPreferences.SPEED_USE_GLOBAL;
                viewBinding.useGlobalCheckbox.setChecked(isGlobal);
                viewBinding.seekBar.updateSpeed(isGlobal ? 1 : speed);
                List<String> skipSilenceValues =
                        Arrays.asList(getContext().getResources().getStringArray(R.array.skip_silence_values));
                int skipSilencePos = skipSilenceValues.indexOf(String.valueOf(skipSilence.code));
                if (skipSilencePos == -1) {
                    viewBinding.skipSilenceSpinner.setSelection(0);
                }
                viewBinding.skipSilenceSpinner.setSelection(skipSilencePos);

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
                            } else {
                                final int pos = viewBinding.skipSilenceSpinner.getSelectedItemPosition();
                                List<String> entryValues =
                                        Arrays.asList(getContext().getResources().getStringArray(R.array.skip_silence_values));
                                final int id = Integer.parseInt(entryValues.get(pos));
                                if (pos >= 0 && pos < entryValues.size()) {
                                    newSkipSilence = FeedPreferences.SkipSilence.fromCode(id);
                                } else {
                                    newSkipSilence = FeedPreferences.SkipSilence.GLOBAL;
                                }
                            }
                            feedPreferences.setFeedSkipSilence(newSkipSilence);
                            DBWriter.setFeedPreferences(feedPreferences);
                            EventBus.getDefault().post(new SpeedPresetChangedEvent(
                                    feedPreferences.getFeedPlaybackSpeed(),
                                    feed.getId(), feedPreferences.getFeedSkipSilence()));
                        })
                        .setNegativeButton(R.string.cancel_label, null)
                        .show();
                return true;
            });
        }

        private void setupEpisodeFilterPreference() {
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
        }

        private void setupAuthentificationPreference() {
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
        }

        private void setupAutoDeletePreference() {
            findPreference(PREF_AUTO_DELETE).setOnPreferenceChangeListener((preference, newValue) -> {
                switch ((String) newValue) {
                    case "global":
                        feedPreferences.setAutoDeleteAction(FeedPreferences.AutoDeleteAction.GLOBAL);
                        break;
                    case "always":
                        feedPreferences.setAutoDeleteAction(FeedPreferences.AutoDeleteAction.ALWAYS);
                        break;
                    case "never":
                        feedPreferences.setAutoDeleteAction(FeedPreferences.AutoDeleteAction.NEVER);
                        break;
                    default:
                }
                DBWriter.setFeedPreferences(feedPreferences);
                updateAutoDeleteSummary();
                return false;
            });
        }

        private void updateAutoDeleteSummary() {
            ListPreference autoDeletePreference = findPreference(PREF_AUTO_DELETE);

            switch (feedPreferences.getAutoDeleteAction()) {
                case GLOBAL:
                    autoDeletePreference.setSummary(R.string.global_default);
                    autoDeletePreference.setValue("global");
                    break;
                case ALWAYS:
                    autoDeletePreference.setSummary(R.string.feed_auto_download_always);
                    autoDeletePreference.setValue("always");
                    break;
                case NEVER:
                    autoDeletePreference.setSummary(R.string.feed_auto_download_never);
                    autoDeletePreference.setValue("never");
                    break;
            }
        }

        private void setupVolumeAdaptationPreferences() {
            ListPreference volumeAdaptationPreference = findPreference("volumeReduction");
            volumeAdaptationPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                switch ((String) newValue) {
                    case "off":
                        feedPreferences.setVolumeAdaptionSetting(VolumeAdaptionSetting.OFF);
                        break;
                    case "light":
                        feedPreferences.setVolumeAdaptionSetting(VolumeAdaptionSetting.LIGHT_REDUCTION);
                        break;
                    case "heavy":
                        feedPreferences.setVolumeAdaptionSetting(VolumeAdaptionSetting.HEAVY_REDUCTION);
                        break;
                    case "light_boost":
                        feedPreferences.setVolumeAdaptionSetting(VolumeAdaptionSetting.LIGHT_BOOST);
                        break;
                    case "medium_boost":
                        feedPreferences.setVolumeAdaptionSetting(VolumeAdaptionSetting.MEDIUM_BOOST);
                        break;
                    case "heavy_boost":
                        feedPreferences.setVolumeAdaptionSetting(VolumeAdaptionSetting.HEAVY_BOOST);
                        break;
                    default:
                }
                DBWriter.setFeedPreferences(feedPreferences);
                updateVolumeAdaptationValue();
                EventBus.getDefault().post(
                        new VolumeAdaptionChangedEvent(feedPreferences.getVolumeAdaptionSetting(), feed.getId()));
                return false;
            });
        }

        private void updateVolumeAdaptationValue() {
            ListPreference volumeAdaptationPreference = findPreference("volumeReduction");

            switch (feedPreferences.getVolumeAdaptionSetting()) {
                case OFF:
                    volumeAdaptationPreference.setValue("off");
                    break;
                case LIGHT_REDUCTION:
                    volumeAdaptationPreference.setValue("light");
                    break;
                case HEAVY_REDUCTION:
                    volumeAdaptationPreference.setValue("heavy");
                    break;
                case LIGHT_BOOST:
                    volumeAdaptationPreference.setValue("light_boost");
                    break;
                case MEDIUM_BOOST:
                    volumeAdaptationPreference.setValue("medium_boost");
                    break;
                case HEAVY_BOOST:
                    volumeAdaptationPreference.setValue("heavy_boost");
                    break;
            }
        }

        private void setupNewEpisodesAction() {
            findPreference(PREF_NEW_EPISODES_ACTION).setOnPreferenceChangeListener((preference, newValue) -> {
                int code = Integer.parseInt((String) newValue);
                feedPreferences.setNewEpisodesAction(FeedPreferences.NewEpisodesAction.fromCode(code));
                DBWriter.setFeedPreferences(feedPreferences);
                updateNewEpisodesAction();
                return false;
            });
        }

        private void updateNewEpisodesAction() {
            ListPreference newEpisodesAction = findPreference(PREF_NEW_EPISODES_ACTION);
            newEpisodesAction.setValue("" + feedPreferences.getNewEpisodesAction().code);

            switch (feedPreferences.getNewEpisodesAction()) {
                case GLOBAL:
                    newEpisodesAction.setSummary(R.string.global_default);
                    break;
                case ADD_TO_INBOX:
                    newEpisodesAction.setSummary(R.string.feed_new_episodes_action_add_to_inbox);
                    break;
                case ADD_TO_QUEUE:
                    newEpisodesAction.setSummary(R.string.feed_new_episodes_action_add_to_queue);
                    break;
                case NOTHING:
                    newEpisodesAction.setSummary(R.string.feed_new_episodes_action_nothing);
                    break;
                default:
            }
        }

        private void setupKeepUpdatedPreference() {
            SwitchPreferenceCompat pref = findPreference("keepUpdated");

            pref.setChecked(feedPreferences.getKeepUpdated());
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = Boolean.TRUE.equals(newValue);
                feedPreferences.setKeepUpdated(checked);
                DBWriter.setFeedPreferences(feedPreferences);
                pref.setChecked(checked);
                return false;
            });
        }

        private void setupAutoDownloadGlobalPreference() {
            if (!UserPreferences.isEnableAutodownload()) {
                SwitchPreferenceCompat autodl = findPreference("autoDownload");
                autodl.setChecked(false);
                autodl.setEnabled(false);
                autodl.setSummary(R.string.auto_download_disabled_globally);
                findPreference(PREF_EPISODE_FILTER).setEnabled(false);
            }
        }

        private void setupAutoDownloadPreference() {
            SwitchPreferenceCompat pref = findPreference("autoDownload");

            pref.setEnabled(UserPreferences.isEnableAutodownload());
            if (UserPreferences.isEnableAutodownload()) {
                pref.setChecked(feedPreferences.getAutoDownload());
            } else {
                pref.setChecked(false);
                pref.setSummary(R.string.auto_download_disabled_globally);
            }

            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = Boolean.TRUE.equals(newValue);

                feedPreferences.setAutoDownload(checked);
                DBWriter.setFeedPreferences(feedPreferences);
                updateAutoDownloadEnabled();
                pref.setChecked(checked);
                return false;
            });
        }

        private void updateAutoDownloadEnabled() {
            if (feed != null && feed.getPreferences() != null) {
                boolean enabled = feed.getPreferences().getAutoDownload() && UserPreferences.isEnableAutodownload();
                findPreference(PREF_EPISODE_FILTER).setEnabled(enabled);
            }
        }

        private void setupTags() {
            findPreference(PREF_TAGS).setOnPreferenceClickListener(preference -> {
                TagSettingsDialog.newInstance(Collections.singletonList(feedPreferences))
                        .show(getChildFragmentManager(), TagSettingsDialog.TAG);
                return true;
            });
        }

        private void setupEpisodeNotificationPreference() {
            SwitchPreferenceCompat pref = findPreference(PREF_NOTIFICATION);

            pref.setChecked(feedPreferences.getShowEpisodeNotification());
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean checked = Boolean.TRUE.equals(newValue);
                if (checked && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(getContext(),
                        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    enableNotificationsRequestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return false;
                }
                feedPreferences.setShowEpisodeNotification(checked);
                DBWriter.setFeedPreferences(feedPreferences);
                pref.setChecked(checked);
                return false;
            });
        }
    }
}
