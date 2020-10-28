package de.danoeh.antennapod.fragment.preferences.about;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.adapter.SimpleIconListAdapter;
import de.danoeh.antennapod.core.util.IntentUtils;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LicensesFragment extends ListFragment {
    private Disposable licensesLoader;
    private final ArrayList<LicenseItem> licenses = new ArrayList<>();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDivider(null);

        licensesLoader = Single.create((SingleOnSubscribe<ArrayList<LicenseItem>>) emitter -> {
            licenses.clear();
            InputStream stream = getContext().getAssets().open("licenses.xml");
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            NodeList libraryList = docBuilder.parse(stream).getElementsByTagName("library");
            for (int i = 0; i < libraryList.getLength(); i++) {
                NamedNodeMap lib = libraryList.item(i).getAttributes();
                licenses.add(new LicenseItem(
                        lib.getNamedItem("name").getTextContent(),
                        String.format("By %s, %s license",
                                lib.getNamedItem("author").getTextContent(),
                                lib.getNamedItem("license").getTextContent()),
                        null,
                        lib.getNamedItem("website").getTextContent(),
                        lib.getNamedItem("licenseText").getTextContent()));
            }
            emitter.onSuccess(licenses);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                developers -> setListAdapter(new SimpleIconListAdapter<LicenseItem>(getContext(), developers)),
                error -> Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_LONG).show()
        );

    }

    private static class LicenseItem extends SimpleIconListAdapter.ListItem {
        final String licenseUrl;
        final String licenseTextFile;

        LicenseItem(String title, String subtitle, String imageUrl, String licenseUrl, String licenseTextFile) {
            super(title, subtitle, imageUrl);
            this.licenseUrl = licenseUrl;
            this.licenseTextFile = licenseTextFile;
        }
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        LicenseItem item = licenses.get(position);
        CharSequence[] items = {"View website", "View license"};
        new AlertDialog.Builder(getContext())
                .setTitle(item.title)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        IntentUtils.openInBrowser(getContext(), item.licenseUrl);
                    } else if (which == 1) {
                        showLicenseText(item.licenseTextFile);
                    }
                }).show();
    }

    private void showLicenseText(String licenseTextFile) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getContext().getAssets().open(licenseTextFile)));
            StringBuilder licenseText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                licenseText.append(line).append("\n");
            }

            new AlertDialog.Builder(getContext())
                    .setMessage(licenseText)
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (licensesLoader != null) {
            licensesLoader.dispose();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.licenses);
    }
}
