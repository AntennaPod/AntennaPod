package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MediaplayerInfoActivity.MediaplayerInfoContentFragment;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;


public class ChaptersFragment extends ListFragment implements MediaplayerInfoContentFragment {

    private static final String TAG = "ChaptersFragment";

    private Playable media;
    private PlaybackController controller;

    private ChaptersListAdapter adapter;

    public static ChaptersFragment newInstance(Playable media) {
        ChaptersFragment f = new ChaptersFragment();
        f.media = media;
        return f;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // add padding
        final ListView lv = getListView();
        lv.setClipToPadding(false);
        final int vertPadding = getResources().getDimensionPixelSize(R.dimen.list_vertical_padding);
        lv.setPadding(0, vertPadding, 0, vertPadding);

        adapter = new ChaptersListAdapter(getActivity(), 0, pos -> {
            if(controller == null) {
                Log.d(TAG, "controller is null");
                return;
            }
            Chapter chapter = (Chapter) getListAdapter().getItem(pos);
            controller.seekToChapter(chapter);
        });
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.setMedia(media);
        adapter.notifyDataSetChanged();
        if(media == null || media.getChapters() == null) {
            setEmptyText(getString(R.string.no_chapters_label));
        } else {
            setEmptyText(null);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        adapter = null;
        controller = null;
    }

    @Override
    public void onMediaChanged(Playable media) {
        if(this.media == media) {
            return;
        }
        this.media = media;
        if (adapter != null) {
            adapter.setMedia(media);
            adapter.notifyDataSetChanged();
            if(media == null || media.getChapters() == null || media.getChapters().size() == 0) {
                setEmptyText(getString(R.string.no_items_label));
            } else {
                setEmptyText(null);
            }
        }
    }

    public void setController(PlaybackController controller) {
        this.controller = controller;
    }

}
