package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.PodcastIndexTranscriptUtils;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.core.util.gui.ShownotesCleaner;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays the description of a Playable object in a Webview.
 */
public class ItemTranscriptTextFragment extends Fragment {
    private static final String TAG = "ItemTranscriptText";

    private RecyclerView recyclerView;
    private Disposable webViewLoader;
    private PlaybackController controller;

    SortedMap<Long, TranscriptSegment> map;
    TreeMap<Long, TranscriptSegment> segmentsMap;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "Creating view");
        View root = inflater.inflate(R.layout.item_transcript_text, container, false);
        recyclerView = root.findViewById(R.id.transcript_text_recyclerview);
        // setting recyclerView layoutManager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL,
                false);
        recyclerView.setLayoutManager(layoutManager);
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment destroyed");
    }

    private void load() {
        Log.d(TAG, "load()");
        if (webViewLoader != null) {
            webViewLoader.dispose();
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        webViewLoader = Maybe.<String>create(emitter -> {
            Playable media = controller.getMedia();
            if (media == null) {
                emitter.onComplete();
                return;
            }
            String transcriptStr = "";
            if (media instanceof FeedMedia) {
                FeedMedia feedMedia = ((FeedMedia) media);
                if (feedMedia.getItem() == null) {
                    feedMedia.setItem(DBReader.getFeedItem(feedMedia.getItemId()));
                }
                Transcript transcript = PodcastIndexTranscriptUtils.loadTranscript(feedMedia);
                if (transcript != null) {
                    segmentsMap = transcript.getSegmentsMap();
                    map = segmentsMap.tailMap(0L, true);
                    Iterator<Long> iter = map.keySet().iterator();
                    try {
                        while (true) {
                            Long l = iter.next();
                            long start = segmentsMap.get(l).getStartTime();
                            transcriptStr = transcriptStr.concat(
                                    "<a id=\"seg" + start + "\">"
                                            + segmentsMap.get(l).getWords()
                                            + "</a> "
                            );
                        }
                    } catch (NoSuchElementException e) {
                        // DONE
                    }
                    Log.d(TAG, "FULL TRANSCRIPT" + transcriptStr);
                }
            }
            ShownotesCleaner shownotesCleaner = new ShownotesCleaner(
                    context, transcriptStr, media.getDuration());
            emitter.onSuccess(shownotesCleaner.processShownotes());
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(data -> {
            // TT TODO: show data
            Log.d(TAG, "Webview loaded with data " + data);
        }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void scrollToPosition(long position) {
        if (segmentsMap == null) {
            return;
        }
        Map.Entry<Long, TranscriptSegment> entry = segmentsMap.floorEntry(position);
        if (entry != null) {
            Log.d(TAG, "Scrolling to position" + position + " jump seg" + Long.toString(entry.getKey()));
            // TT TODO WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                load();
            }
        };
        controller.init();
        load();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (webViewLoader != null) {
            webViewLoader.dispose();
        }
        controller.release();
        controller = null;
    }
}
