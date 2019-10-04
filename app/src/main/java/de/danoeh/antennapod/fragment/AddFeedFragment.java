package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.activity.OpmlImportFromPathActivity;
import de.danoeh.antennapod.fragment.gpodnet.GpodnetMainFragment;

/**
 * Provides actions for adding new podcast subscriptions
 */
public class AddFeedFragment extends Fragment {

    public static final String TAG = "AddFeedFragment";

    /**
     * Preset value for url text field.
     */
    private static final String ARG_FEED_URL = "feedurl";

    private EditText combinedFeedSearchBox;
    private MainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.addfeed, container, false);

        activity = (MainActivity) getActivity();
        activity.getSupportActionBar().setTitle(R.string.add_feed_label);

        setupAdvancedSearchButtons(root);
        setupSeachBox(root);

        View butOpmlImport = root.findViewById(R.id.btn_opml_import);
        butOpmlImport.setOnClickListener(v -> startActivity(new Intent(getActivity(),
                OpmlImportFromPathActivity.class)));

        return root;
    }

    private void setupSeachBox(View root) {
        final EditText etxtFeedurl = root.findViewById(R.id.etxtFeedurl);

        Bundle args = getArguments();
        if (args != null && args.getString(ARG_FEED_URL) != null) {
            etxtFeedurl.setText(args.getString(ARG_FEED_URL));
        }

        Button butConfirmAddUrl = root.findViewById(R.id.butConfirm);
        butConfirmAddUrl.setOnClickListener(v -> {
            addUrl(etxtFeedurl.getText().toString());
        });

        combinedFeedSearchBox = root.findViewById(R.id.combinedFeedSearchBox);
        combinedFeedSearchBox.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    private void setupAdvancedSearchButtons(View root) {
        View butAdvancedSearch = root.findViewById(R.id.advanced_search);
        registerForContextMenu(butAdvancedSearch);
        butAdvancedSearch.setOnClickListener(v -> butAdvancedSearch.showContextMenu());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.advanced_search, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_fyyd:
                activity.loadChildFragment(new FyydSearchFragment());
                return true;
            case R.id.search_gpodder:
                activity.loadChildFragment(new GpodnetMainFragment());
                return true;
            case R.id.search_itunes:
                activity.loadChildFragment(new ItunesSearchFragment());
                return true;
        }
        return false;
    }


    private void addUrl(String url) {
        Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
        intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, url);
        intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, getString(R.string.add_feed_label));
        startActivity(intent);
    }

    private void performSearch() {
        String query = combinedFeedSearchBox.getText().toString();

        if (query.startsWith("http")) {
            addUrl(query);
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(CombinedSearchFragment.ARGUMENT_QUERY, query);
        CombinedSearchFragment fragment = new CombinedSearchFragment();
        fragment.setArguments(bundle);
        activity.loadChildFragment(fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // So, we certainly *don't* have an options menu,
        // but unless we say we do, old options menus sometimes
        // persist.  mfietz thinks this causes the ActionBar to be invalidated
        setHasOptionsMenu(true);
    }
}
