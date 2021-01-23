package de.danoeh.antennapod.fragment.preferences.about;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import de.danoeh.antennapod.adapter.SimpleIconListAdapter;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DevelopersFragment extends ListFragment {
    private Disposable developersLoader;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDivider(null);
        getListView().setSelector(android.R.color.transparent);

        developersLoader = Single.create((SingleOnSubscribe<ArrayList<SimpleIconListAdapter.ListItem>>) emitter -> {
            ArrayList<SimpleIconListAdapter.ListItem> developers = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getContext().getAssets().open("developers.csv")));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] info = line.split(";");
                developers.add(new SimpleIconListAdapter.ListItem(info[0], info[2],
                        "https://avatars2.githubusercontent.com/u/" + info[1] + "?s=60&v=4"));
            }
            emitter.onSuccess(developers);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                developers -> setListAdapter(new SimpleIconListAdapter<>(getContext(), developers)),
                error -> Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_LONG).show()
        );

    }

    @Override
    public void onStop() {
        super.onStop();
        if (developersLoader != null) {
            developersLoader.dispose();
        }
    }
}
