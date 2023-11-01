package de.danoeh.antennapod.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import de.danoeh.antennapod.ItemTranscriptRVAdapter;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.PodcastIndexTranscriptUtils;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.core.util.gui.ShownotesCleaner;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.placeholder.PlaceholderContent;
import de.danoeh.antennapod.view.NestedScrollableHost;
import de.danoeh.antennapod.view.ShownotesWebView;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays the description of a Playable object in a Webview.
 */
public class ItemTranscriptRVFragment extends Fragment {
    private static final String TAG = "TranscriptRVFragment";
    RecyclerView rv;
    private Disposable webViewLoader;

    private PlaybackController controller;

    Transcript transcript;
    SortedMap<Long, TranscriptSegment> map;
    TreeMap<Long, TranscriptSegment> segmentsMap;
    ItemTranscriptRVAdapter adapter = null;
    View currentView = null;
    View prevView = null;
    Color prevColor;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "Creating view");
        View root = inflater.inflate(R.layout.fragment_item_transcript_rv_list, container, false);
        rv = root.findViewById(R.id.transcript_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setRecycleChildrenOnDetach(true);
        rv.setLayoutManager(layoutManager);

        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                load();
            }
        };
        controller.init();
        // Set the adapter
        if (rv instanceof RecyclerView) {
            Context context = rv.getContext();
            RecyclerView recyclerView = (RecyclerView) rv;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            Playable media = controller.getMedia();
            if (media != null && media instanceof FeedMedia) {
                FeedMedia feedMedia = ((FeedMedia) media);
                if (feedMedia.getItem() == null) {
                    feedMedia.setItem(DBReader.getFeedItem(feedMedia.getItemId()));
                }

                transcript = PodcastIndexTranscriptUtils.loadTranscript(feedMedia);

                adapter = new ItemTranscriptRVAdapter(transcript);
                recyclerView.setAdapter(adapter);
            }
        }

        // TT TODO
        //registerForContextMenu(webvDescription);
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment destroyed");
        if (rv!= null) {
            rv.removeAllViews();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TT TODO
        return true;
    }

    private void load() {
        Log.d(TAG, "load()");
        /*
        if (webViewLoader != null) {
            webViewLoader.dispose();
        }
        */
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

                        transcript = PodcastIndexTranscriptUtils.loadTranscript(feedMedia);
                        if (transcript != null) {
                            adapter.setTranscript(transcript);
                            adapter.notifyDataSetChanged();

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
                    // TT TODO load data
                    Log.d(TAG, "Webview loaded with data " + data);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @SuppressLint("ResourceAsColor")
    public void scrollToPosition(long position) {
        if (segmentsMap == null) {
            return;
        }
        Map.Entry<Long, TranscriptSegment> entry = segmentsMap.floorEntry(position);
        if (entry != null) {
            Integer pos = adapter.positions.get(entry.getKey());
            if (pos != null) {
                Log.d(TAG, "Scrolling to position" + pos + " jump " + Long.toString(entry.getKey()));
                    LinearSmoothScroller smoothScroller=new LinearSmoothScroller(getActivity()){
                        @Override
                        protected int getVerticalSnapPreference() {
                            return LinearSmoothScroller.SNAP_TO_START;
                        }
                    };

                    smoothScroller.setTargetPosition(pos);  // pos on which item you want to scroll recycler view
                    rv.getLayoutManager().startSmoothScroll(smoothScroller);
                    rv.scrollTo(0, 0);

               prevView = currentView;
               currentView = rv.getLayoutManager().findViewByPosition(pos);
               rv.getLayoutManager().findViewByPosition(pos);
               if (currentView != null) {
                   currentView.setBackgroundColor(R.color.light_gray);
                    if (prevView != null && prevView != currentView) {
                       prevView.setBackgroundColor(R.color.background_light);
                    }
               }
            }
        }
    }

    public void scrollToTop() {
        rv.getLayoutManager().scrollToPosition(0);
    }

    @Override
    public void onStart() {
        super.onStart();

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

    View getWebview() {
        return rv;
    }
}
