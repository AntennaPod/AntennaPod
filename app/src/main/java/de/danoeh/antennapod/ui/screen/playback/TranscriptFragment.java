package de.danoeh.antennapod.ui.screen.playback;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.chapters.PodcastIndexTranscriptUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.Transcript;
import de.danoeh.antennapod.model.feed.TranscriptSegment;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class TranscriptFragment extends AppCompatDialogFragment  {
    public static final String TAG = "TranscriptFragment";
    RecyclerView rv;
    private ProgressBar progressBar;

    private PlaybackController controller;

    Transcript transcript;
    SortedMap<Long, TranscriptSegment> map;
    TreeMap<Long, TranscriptSegment> segmentsMap;
    ItemTranscriptRvAdapter adapter = null;
    View currentView = null;
    View prevView = null;
    Color prevColor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public void onResume() {
        ViewGroup.LayoutParams params;
        params = getDialog().getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((WindowManager.LayoutParams) params);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        super.onResume();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(onCreateView(getLayoutInflater()))
                .setPositiveButton(getString(R.string.close_label), null) //dismisses
                .create();
        dialog.show();

        return dialog;
    }

    public View onCreateView(LayoutInflater inflater) {
        Log.d(TAG, "Creating view");
        View root = inflater.inflate(R.layout.fragment_item_transcript_rv_list, null, false);
        rv = root.findViewById(R.id.transcript_list);
        rv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked");

            }
        });

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
                if (transcript != null) {
                    segmentsMap = transcript.getSegmentsMap();
                    adapter = new ItemTranscriptRvAdapter(transcript);
                    adapter.setController(controller);
                    recyclerView.setAdapter(adapter);
                }

            }
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public void loadMediaInfo() {
                // TODO
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment destroyed");
        if (rv != null) {
            rv.removeAllViews();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        Log.d(TAG, "onEventMainThread TranscriptFragment " + event.getPosition());
        scrollToPosition(event.getPosition());
    }


    private void load() {
        Log.d(TAG, "load()");
        Context context = getContext();
        if (context == null) {
            return;
        }
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
                final LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getActivity()) {
                    @Override
                    protected int getVerticalSnapPreference() {
                        return LinearSmoothScroller.SNAP_TO_START;
                    }
                };

                rv.scrollToPosition(0);
                rv.getLayoutManager().scrollToPosition(pos);
                rv.setTop(pos - 1);
                smoothScroller.setTargetPosition(pos - 1);  // pos on which item you want to scroll recycler view
                rv.getLayoutManager().startSmoothScroll(smoothScroller);

                View nextView = rv.getLayoutManager().findViewByPosition(pos);
                if (nextView != null && nextView != currentView) {
                    prevView = currentView;
                    currentView = nextView;
                }
                if (currentView != null) {
                    ((TextView) currentView.findViewById(R.id.content)).setTypeface(null, Typeface.BOLD);
                    ((TextView) currentView.findViewById(R.id.content)).setTextColor(
                            ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorPrimary)
                    );
                }

                if (prevView != null && prevView != currentView && currentView != null) {
                    ((TextView) prevView.findViewById(R.id.content)).setTypeface(null, Typeface.NORMAL);
                    ((TextView) prevView.findViewById(R.id.content)).setTextColor(
                            ThemeUtils.getColorFromAttr(getContext(), android.R.attr.textColorSecondary));
                }
            }
        }
    }

    public void scrollToTop() {
        rv.getLayoutManager().scrollToPosition(0);
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

}