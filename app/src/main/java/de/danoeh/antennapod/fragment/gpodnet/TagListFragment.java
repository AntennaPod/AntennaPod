package de.danoeh.antennapod.fragment.gpodnet;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.gpodnet.TagListAdapter;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetTag;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TagListFragment extends ListFragment {
    private static final int COUNT = 50;
    private static final String TAG = "TagListFragment";
    private Disposable disposable;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener((parent, view1, position, id) -> {
            GpodnetTag tag = (GpodnetTag) getListAdapter().getItem(position);
            ((MainActivity) getActivity()).loadChildFragment(TagFragment.newInstance(tag));
        });

        startLoadTask();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void startLoadTask() {
        if (disposable != null) {
            disposable.dispose();
        }
        setListShown(false);
        disposable = Observable.fromCallable(
            () -> {
                GpodnetService service = new GpodnetService(AntennapodHttpClient.getHttpClient(),
                        GpodnetPreferences.getHosturl());
                return service.getTopTags(COUNT);
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    tags -> {
                        setListAdapter(new TagListAdapter(getContext(), android.R.layout.simple_list_item_1, tags));
                        setListShown(true);
                    }, error -> {
                        TextView txtvError = new TextView(getActivity());
                        txtvError.setText(error.getMessage());
                        getListView().setEmptyView(txtvError);
                        setListShown(true);
                        Log.e(TAG, Log.getStackTraceString(error));
                    });
    }
}

