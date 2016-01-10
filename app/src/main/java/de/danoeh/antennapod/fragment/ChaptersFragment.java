package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.AudioplayerActivity.AudioplayerContentFragment;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.util.playback.Playable;
import de.danoeh.antennapod.core.util.playback.PlaybackController;


public class ChaptersFragment extends ListFragment implements AudioplayerContentFragment {

    private Playable media;
    private PlaybackController controller;

    private ChaptersListAdapter adapter;

    public static ChaptersFragment newInstance(Playable media, PlaybackController controller) {
        ChaptersFragment f = new ChaptersFragment();
        f.media = media;
        f.controller = controller;
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

    @Override
    public void onDataSetChanged(Playable media) {
        adapter.setMedia(media);
        adapter.notifyDataSetChanged();
        if(media.getChapters() == null) {
            setEmptyText(getString(R.string.no_items_label));
        } else {
            setEmptyText(null);
        }
    }
}
