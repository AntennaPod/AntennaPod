package de.danoeh.antennapod.ui.preferences.screen.about;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import de.danoeh.antennapod.ui.common.IntentUtils;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SpecialThanksFragment extends ListFragment {
    private Disposable translatorsLoader;

    private ArrayList<SpecialMemberItem> specialMembers = new ArrayList<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDivider(null);

        translatorsLoader = Single.create((SingleOnSubscribe<ArrayList<SpecialMemberItem>>) emitter -> {
            specialMembers.clear();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getContext().getAssets().open("special_thanks.csv"), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] info = line.split(";");
                specialMembers.add(new SpecialMemberItem(info[0], info[1], info[2], info[3]));
            }
            emitter.onSuccess(specialMembers);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                translators -> setListAdapter(new SimpleIconListAdapter<>(getContext(), translators)),
                error -> Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private static class SpecialMemberItem extends SimpleIconListAdapter.ListItem {
        final String githubUsername;

        SpecialMemberItem(String title, String subtitle, String imageUrl, String gitHubUsername) {
            super(title, subtitle, imageUrl);
            this.githubUsername = gitHubUsername;
        }
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        IntentUtils.openInBrowser(getContext(), "https://github.com/" + specialMembers.get(position).githubUsername);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (translatorsLoader != null) {
            translatorsLoader.dispose();
        }
    }
}
