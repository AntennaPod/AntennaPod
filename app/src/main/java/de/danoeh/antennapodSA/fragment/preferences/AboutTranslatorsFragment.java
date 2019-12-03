package de.danoeh.antennapodSA.fragment.preferences;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import de.danoeh.antennapodSA.R;
import de.danoeh.antennapodSA.activity.PreferenceActivity;
import de.danoeh.antennapodSA.adapter.SimpleIconListAdapter;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AboutTranslatorsFragment extends ListFragment {
    private Disposable translatorsLoader;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDivider(null);
        getListView().setSelector(android.R.color.transparent);

        translatorsLoader = Single.create((SingleOnSubscribe<ArrayList<SimpleIconListAdapter.ListItem>>) emitter -> {
            ArrayList<SimpleIconListAdapter.ListItem> translators = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getContext().getAssets().open("translators.csv")));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] info = line.split(";");
                translators.add(new SimpleIconListAdapter.ListItem(info[0], info[1], null));
            }
            emitter.onSuccess(translators);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                translators -> setListAdapter(new SimpleIconListAdapter<>(getContext(), translators)),
                error -> Toast.makeText(getContext(), "Error while loading translators", Toast.LENGTH_LONG).show()
        );

    }

    @Override
    public void onStop() {
        super.onStop();
        if (translatorsLoader != null) {
            translatorsLoader.dispose();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.translators);
    }
}
