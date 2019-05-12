package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;
import de.danoeh.antennapod.core.event.ServiceEvent;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;
import de.greenrobot.event.EventBus;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class ChaptersFragment extends ListFragment {
    private static final String TAG = "ChaptersFragment";
    private ChaptersListAdapter adapter;
    private PlaybackController controller;
    private Disposable disposable;


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        adapter = new ChaptersListAdapter(getActivity(), 0, pos -> {
            Chapter chapter = (Chapter) getListAdapter().getItem(pos);
            controller.seekToChapter(chapter);
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
        loadMediaInfo();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        controller.release();
        controller = null;
    }

    public void onEventMainThread(ServiceEvent event) {
        if (event.action == ServiceEvent.Action.SERVICE_STARTED && controller != null) {
            controller.init();
        }
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
        if (adapter != null) {
            adapter.setMedia(media);
            adapter.notifyDataSetChanged();
            if (media == null || media.getChapters() == null || media.getChapters().size() == 0) {
                setEmptyText(getString(R.string.no_chapters_label));
            } else {
                setEmptyText(null);
            }
        }
    }
}
