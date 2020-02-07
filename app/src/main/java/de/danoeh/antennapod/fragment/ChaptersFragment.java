package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.List;
import java.util.ListIterator;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;
import de.danoeh.antennapod.adapter.QueueRecyclerAdapter;
import de.danoeh.antennapod.core.event.PlaybackPositionEvent;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.service.playback.PlayerStatus;
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

public class ChaptersFragment extends ListFragment {
    private static final String TAG = "ChaptersFragment";
    private ChaptersListAdapter adapter;
    private PlaybackController controller;
    private Disposable disposable;
    private int focusedChapter = -1;
    private Playable media;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        EmptyViewHandler emptyView = new EmptyViewHandler(getContext());
        emptyView.attachToListView(lv);
        emptyView.setIcon(R.attr.ic_bookmark);
        emptyView.setTitle(R.string.no_chapters_head_label);
        emptyView.setMessage(R.string.no_chapters_label);

        adapter = new ChaptersListAdapter(getActivity(), 0, pos -> {
            if (controller.getStatus() != PlayerStatus.PLAYING) {
                controller.playPause();
            }
            Chapter chapter = (Chapter) getListAdapter().getItem(pos);
            controller.seekToChapter(chapter);
            updateChapterSelection(pos);
        });
        setListAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        controller = new PlaybackController(getActivity(), false) {
            @Override
            public boolean loadMediaInfo() {
                ChaptersFragment.this.loadMediaInfo();
                return true;
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
    public void onDestroyView() {
        super.onDestroyView();

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        controller.release();
        controller = null;
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        updateChapterSelection(getCurrentChapter(media));
    }

    private int getCurrentChapter(Playable media) {
        if (media == null || media.getChapters() == null || media.getChapters().size() == 0 || controller == null) {
            return -1;
        }
        int currentPosition = controller.getPosition();

        List<Chapter> chapters = media.getChapters();
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getStart() > currentPosition) {
                return i - 1;
            }
        }
        return chapters.size() - 1;
    }

    private void loadMediaInfo() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Maybe.create(emitter -> {
            Playable media = controller.getMedia();
            if (media != null) {
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
        adapter.notifyDataSetChanged();
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
            if (getListView().getFirstVisiblePosition() >= position
                    || getListView().getLastVisiblePosition() <= position) {
                getListView().setSelectionFromTop(position, 100);
            }
        }
    }
}
