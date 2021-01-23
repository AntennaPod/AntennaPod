package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.danoeh.antennapod.view.EmptyViewHandler;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ChaptersFragment extends Fragment {
    private static final String TAG = "ChaptersFragment";
    private ChaptersListAdapter adapter;
    private PlaybackController controller;
    private Disposable disposable;
    private int focusedChapter = -1;
    private Playable media;
    private LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.simple_list_fragment, container, false);
        root.findViewById(R.id.toolbar).setVisibility(View.GONE);
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity()).build());

        adapter = new ChaptersListAdapter(getActivity(), pos -> {
            if (controller.getStatus() != PlayerStatus.PLAYING) {
                controller.playPause();
            }
            Chapter chapter = adapter.getItem(pos);
            controller.seekToChapter(chapter);
            updateChapterSelection(pos);
        });
        recyclerView.setAdapter(adapter);

        EmptyViewHandler emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToRecyclerView(recyclerView);
        emptyView.setIcon(R.attr.ic_bookmark);
        emptyView.setTitle(R.string.no_chapters_head_label);
        emptyView.setMessage(R.string.no_chapters_label);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity()) {
            @Override
            public boolean loadMediaInfo() {
                ChaptersFragment.this.loadMediaInfo();
                return true;
            }

            @Override
            public void setupGUI() {
                ChaptersFragment.this.loadMediaInfo();
            }

            @Override
            public void onPositionObserverUpdate() {
                adapter.notifyDataSetChanged();
            }
        };
        controller.init();
        EventBus.getDefault().register(this);
        loadMediaInfo();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (disposable != null) {
            disposable.dispose();
        }
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        updateChapterSelection(getCurrentChapter(media));
        adapter.notifyTimeChanged(event.getPosition());
    }

    private int getCurrentChapter(Playable media) {
        if (controller == null) {
            return -1;
        }
        return ChapterUtils.getCurrentChapterIndex(media, controller.getPosition());
    }

    private void loadMediaInfo() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.create(emitter -> {
            Playable media = controller.getMedia();
            if (media != null) {
                media.loadChapterMarks(getContext());
                emitter.onSuccess(media);
            } else {
                emitter.onComplete();
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(media -> onMediaChanged((Playable) media),
                error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void onMediaChanged(Playable media) {
        this.media = media;
        focusedChapter = -1;
        if (adapter == null) {
            return;
        }
        adapter.setMedia(media);
        ((AudioPlayerFragment) getParentFragment()).setHasChapters(adapter.getItemCount() > 0);
        int positionOfCurrentChapter = getCurrentChapter(media);
        updateChapterSelection(positionOfCurrentChapter);
    }

    private void updateChapterSelection(int position) {
        if (adapter == null) {
            return;
        }

        if (position != -1 && focusedChapter != position) {
            focusedChapter = position;
            adapter.notifyChapterChanged(focusedChapter);
            if (layoutManager.findFirstCompletelyVisibleItemPosition() >= position
                    || layoutManager.findLastCompletelyVisibleItemPosition() <= position) {
                layoutManager.scrollToPositionWithOffset(position, 100);
            }
        }
    }
}
