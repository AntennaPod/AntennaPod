package de.test.antennapod.ui;

import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;

import com.robotium.solo.Condition;
import com.robotium.solo.Solo;
import com.robotium.solo.Timeout;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;

public class PreferencesTest extends ActivityInstrumentationTestCase2<PreferenceActivity>  {

    private static final String TAG = "PreferencesTest";

    private Solo solo;
    private Context context;
    private Resources res;

    public PreferencesTest() {
        super(PreferenceActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
        Timeout.setSmallTimeout(500);
        Timeout.setLargeTimeout(1000);
        context = getInstrumentation().getTargetContext();
        res = getActivity().getResources();
        UserPreferences.init(context);
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    public void testSwitchTheme() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if(theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        solo.clickOnText(solo.getString(R.string.pref_set_theme_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(otherTheme));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getTheme() != theme;
            }
        }, Timeout.getLargeTimeout());
    }

    public void testSwitchThemeBack() {
        final int theme = UserPreferences.getTheme();
        int otherTheme;
        if(theme == de.danoeh.antennapod.core.R.style.Theme_AntennaPod_Light) {
            otherTheme = R.string.pref_theme_title_dark;
        } else {
            otherTheme = R.string.pref_theme_title_light;
        }
        solo.clickOnText(solo.getString(R.string.pref_set_theme_title));
        solo.waitForDialogToOpen(1000);
        solo.clickOnText(solo.getString(otherTheme));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getTheme() != theme;
            }
        }, Timeout.getLargeTimeout());
    }

    public void testExpandNotification() {
        final int priority = UserPreferences.getNotifyPriority();
        solo.clickOnText(solo.getString(R.string.pref_expandNotify_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return priority != UserPreferences.getNotifyPriority();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_expandNotify_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return priority == UserPreferences.getNotifyPriority();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testEnablePersistentPlaybackControls() {
        final boolean persistNotify = UserPreferences.isPersistNotify();
        solo.clickOnText(solo.getString(R.string.pref_persistNotify_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return persistNotify != UserPreferences.isPersistNotify();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_persistNotify_title));
        solo.waitForCondition(new Condition() {
            @Override public boolean isSatisfied() {
                return persistNotify == UserPreferences.isPersistNotify();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testEnqueueAtFront() {
        final boolean enqueueAtFront = UserPreferences.enqueueAtFront();
        solo.clickOnText(solo.getString(R.string.pref_queueAddToFront_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return enqueueAtFront != UserPreferences.enqueueAtFront();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_queueAddToFront_title));
        solo.waitForCondition(new Condition() {
            @Override public boolean isSatisfied() {
                return enqueueAtFront == UserPreferences.enqueueAtFront();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testHeadPhonesDisconnect() {
        final boolean pauseOnHeadsetDisconnect = UserPreferences.isPauseOnHeadsetDisconnect();
        solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
        solo.waitForCondition(new Condition() {
            @Override public boolean isSatisfied() {
                return pauseOnHeadsetDisconnect != UserPreferences.isPauseOnHeadsetDisconnect();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
        solo.waitForCondition(new Condition() {
            @Override public boolean isSatisfied() {
                return pauseOnHeadsetDisconnect == UserPreferences.isPauseOnHeadsetDisconnect();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testHeadPhonesReconnect() {
        if(UserPreferences.isPauseOnHeadsetDisconnect() == false) {
            solo.clickOnText(solo.getString(R.string.pref_pauseOnHeadsetDisconnect_title));
            solo.waitForCondition(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return UserPreferences.isPauseOnHeadsetDisconnect();
                }
            }, Timeout.getLargeTimeout());
        }
        final boolean unpauseOnHeadsetReconnect = UserPreferences.isUnpauseOnHeadsetReconnect();
        solo.clickOnText(solo.getString(R.string.pref_unpauseOnHeadsetReconnect_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return unpauseOnHeadsetReconnect != UserPreferences.isUnpauseOnHeadsetReconnect();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_unpauseOnHeadsetReconnect_title));
        solo.waitForCondition(new Condition() {
            @Override public boolean isSatisfied() {
                return unpauseOnHeadsetReconnect == UserPreferences.isUnpauseOnHeadsetReconnect();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testContinuousPlayback() {
        final boolean continuousPlayback = UserPreferences.isFollowQueue();
        solo.clickOnText(solo.getString(R.string.pref_followQueue_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return continuousPlayback != UserPreferences.isFollowQueue();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_followQueue_title));
        solo.waitForCondition(new Condition() {
            @Override public boolean isSatisfied() {
                return continuousPlayback == UserPreferences.isFollowQueue();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testAutoDelete() {
        final boolean autoDelete = UserPreferences.isAutoDelete();
        solo.clickOnText(solo.getString(R.string.pref_auto_delete_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return autoDelete != UserPreferences.isAutoDelete();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_auto_delete_title));
        solo.waitForCondition(new Condition() {
            @Override public boolean isSatisfied() {
                return autoDelete == UserPreferences.isAutoDelete();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testPlaybackSpeeds() {
        solo.clickOnText(solo.getString(R.string.pref_playback_speed_title));
        solo.waitForDialogToOpen(1000);
        assertTrue(solo.searchText(solo.getString(R.string.no_playback_plugin_title)));
        solo.clickOnText(solo.getString(R.string.close_label));
        solo.waitForDialogToClose(1000);
    }

    public void testPauseForInterruptions() {
        final boolean pauseForFocusLoss = UserPreferences.shouldPauseForFocusLoss();
        solo.clickOnText(solo.getString(R.string.pref_pausePlaybackForFocusLoss_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return pauseForFocusLoss != UserPreferences.shouldPauseForFocusLoss();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_pausePlaybackForFocusLoss_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return pauseForFocusLoss == UserPreferences.shouldPauseForFocusLoss();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testDisableUpdateInterval() {
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_sum));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_Disable));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getUpdateInterval() == 0;
            }
        }, 1000);
    }

    public void testSetUpdateInterval() {
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(solo.getString(R.string.pref_autoUpdateIntervallOrTime_Interval));
        solo.waitForDialogToOpen();
        String search = "12 " + solo.getString(R.string.pref_update_interval_hours_plural);
        solo.clickOnText(search);
        solo.waitForDialogToClose();
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getUpdateInterval() == 12;
            }
        }, Timeout.getLargeTimeout());
    }

    public void testMobileUpdates() {
        final boolean mobileUpdates = UserPreferences.isAllowMobileUpdate();
        solo.clickOnText(solo.getString(R.string.pref_mobileUpdate_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return mobileUpdates != UserPreferences.isAllowMobileUpdate();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_mobileUpdate_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return mobileUpdates == UserPreferences.isAllowMobileUpdate();
            }
        }, Timeout.getLargeTimeout());
    }

    public void testSetSequentialDownload() {
        solo.clickOnText(solo.getString(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "1");
        solo.clickOnText(solo.getString(android.R.string.ok));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getParallelDownloads() == 1;
            }
        }, Timeout.getLargeTimeout());
    }

    public void testSetParallelDownloads() {
        solo.clickOnText(solo.getString(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "10");
        solo.clickOnText(solo.getString(android.R.string.ok));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getParallelDownloads() == 10;
            }
        }, Timeout.getLargeTimeout());
    }

    public void testSetParallelDownloadsInvalidInput() {
        solo.clickOnText(solo.getString(R.string.pref_parallel_downloads_title));
        solo.waitForDialogToOpen();
        solo.clearEditText(0);
        solo.enterText(0, "0");
        assertEquals("1", solo.getEditText(0).getText().toString());
        solo.clearEditText(0);
        solo.enterText(0, "100");
        assertEquals("50", solo.getEditText(0).getText().toString());
    }

    public void testSetEpisodeCache() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String entry = entries[entries.length/2];
        final int value = Integer.valueOf(values[values.length/2]);
        solo.clickOnText(solo.getString(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(entry);
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getEpisodeCacheSize() == value;
            }
        }, Timeout.getLargeTimeout());
    }

    public void testSetEpisodeCacheMin() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String minEntry = entries[0];
        final int minValue = Integer.valueOf(values[0]);
        solo.clickOnText(solo.getString(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen(1000);
        solo.scrollUp();
        solo.clickOnText(minEntry);
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getEpisodeCacheSize() == minValue;
            }
        }, Timeout.getLargeTimeout());
    }


    public void testSetEpisodeCacheMax() {
        String[] entries = res.getStringArray(R.array.episode_cache_size_entries);
        String[] values = res.getStringArray(R.array.episode_cache_size_values);
        String maxEntry = entries[entries.length-1];
        final int maxValue = Integer.valueOf(values[values.length-1]);
        solo.clickOnText(solo.getString(R.string.pref_episode_cache_title));
        solo.waitForDialogToOpen();
        solo.clickOnText(maxEntry);
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.getEpisodeCacheSize() == maxValue;
            }
        }, Timeout.getLargeTimeout());
    }

    public void testAutomaticDownload() {
        final boolean automaticDownload = UserPreferences.isEnableAutodownload();
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.waitForText(solo.getString(R.string.pref_automatic_download_title));
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return automaticDownload != UserPreferences.isEnableAutodownload();
            }
        }, Timeout.getLargeTimeout());
        if(UserPreferences.isEnableAutodownload() == false) {
            solo.clickOnText(solo.getString(R.string.pref_automatic_download_title));
        }
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return UserPreferences.isEnableAutodownload() == true;
            }
        }, Timeout.getLargeTimeout());
        final boolean enableAutodownloadOnBattery = UserPreferences.isEnableAutodownloadOnBattery();
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_on_battery_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return enableAutodownloadOnBattery != UserPreferences.isEnableAutodownloadOnBattery();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_on_battery_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return enableAutodownloadOnBattery == UserPreferences.isEnableAutodownloadOnBattery();
            }
        }, Timeout.getLargeTimeout());
        final boolean enableWifiFilter = UserPreferences.isEnableAutodownloadWifiFilter();
        solo.clickOnText(solo.getString(R.string.pref_autodl_wifi_filter_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return enableWifiFilter != UserPreferences.isEnableAutodownloadWifiFilter();
            }
        }, Timeout.getLargeTimeout());
        solo.clickOnText(solo.getString(R.string.pref_automatic_download_on_battery_title));
        solo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return enableWifiFilter == UserPreferences.isEnableAutodownloadWifiFilter();
            }
        }, Timeout.getLargeTimeout());
    }
}
