package de.danoeh.antennapod.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.core.util.playback.Timeline;
import de.danoeh.antennapod.view.ShownotesWebView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays the description of a Playable object in a Webview.
 */
public class ItemDescriptionFragment extends Fragment {
    private static final String TAG = "ItemDescriptionFragment";

    private static final String PREF = "ItemDescriptionFragmentPrefs";
    private static final String PREF_SCROLL_Y = "prefScrollY";
    private static final String PREF_PLAYABLE_ID = "prefPlayableId";

    private ShownotesWebView webvDescription;
    private Disposable webViewLoader;
    private PlaybackController controller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "Creating view");
        webvDescription = new ShownotesWebView(getActivity().getApplicationContext());
        webvDescription.setTimecodeSelectedListener(time -> {
            if (controller != null) {
                controller.seekTo(time);
            }
        });
        webvDescription.setPageFinishedListener(() -> {
            // Restoring the scroll position might not always work
            webvDescription.postDelayed(ItemDescriptionFragment.this::restoreFromPreference, 50);
        });
        registerForContextMenu(webvDescription);
        return webvDescription;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment destroyed");
        if (webViewLoader != null) {
            webViewLoader.dispose();
        }
        if (webvDescription != null) {
            webvDescription.removeAllViews();
            webvDescription.destroy();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return webvDescription.onContextItemSelected(item);
    }

    private void load() {
        Log.d(TAG, "load()");
        if (webViewLoader != null) {
            webViewLoader.dispose();
        }
        webViewLoader = Observable.fromCallable(this::loadData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    webvDescription.loadDataWithBaseURL("https://127.0.0.1", data, "text/html",
                            "utf-8", "about:blank");
                    Log.d(TAG, "Webview loaded");
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @NonNull
    private String loadData() {
        Timeline timeline = new Timeline(getActivity(), controller.getMedia());
        return timeline.processShownotes();
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreference();
    }

    private void savePreference() {
        Log.d(TAG, "Saving preferences");
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (controller != null && controller.getMedia() != null && webvDescription != null) {
            Log.d(TAG, "Saving scroll position: " + webvDescription.getScrollY());
            editor.putInt(PREF_SCROLL_Y, webvDescription.getScrollY());
            editor.putString(PREF_PLAYABLE_ID, controller.getMedia().getIdentifier()
                    .toString());
        } else {
            Log.d(TAG, "savePreferences was called while media or webview was null");
            editor.putInt(PREF_SCROLL_Y, -1);
            editor.putString(PREF_PLAYABLE_ID, "");
        }
        editor.apply();
    }

    private boolean restoreFromPreference() {
        Log.d(TAG, "Restoring from preferences");
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences(PREF, Activity.MODE_PRIVATE);
            String id = prefs.getString(PREF_PLAYABLE_ID, "");
            int scrollY = prefs.getInt(PREF_SCROLL_Y, -1);
            if (controller != null && scrollY != -1 && controller.getMedia() != null
                    && id.equals(controller.getMedia().getIdentifier().toString())
                    && webvDescription != null) {
                Log.d(TAG, "Restored scroll Position: " + scrollY);
                webvDescription.scrollTo(webvDescription.getScrollX(),
                        scrollY);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity(), false) {
            @Override
            public boolean loadMediaInfo() {
                if (getMedia() == null) {
                    return false;
                }
                load();
                return true;
            }

        };
        controller.init();
        load();
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
    }
}
