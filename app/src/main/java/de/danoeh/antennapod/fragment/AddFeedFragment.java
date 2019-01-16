package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.OnlineFeedViewActivity;
import de.danoeh.antennapod.activity.OpmlImportFromPathActivity;
import de.danoeh.antennapod.activity.SingleFileAddActivity;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.addfeed, container, false);

        final EditText etxtFeedurl = root.findViewById(R.id.etxtFeedurl);

        Bundle args = getArguments();
        if (args != null && args.getString(ARG_FEED_URL) != null) {
            etxtFeedurl.setText(args.getString(ARG_FEED_URL));
        }

        Button butSearchITunes = root.findViewById(R.id.butSearchItunes);
        Button butBrowserGpoddernet = root.findViewById(R.id.butBrowseGpoddernet);
        Button butSearchFyyd = root.findViewById(R.id.butSearchFyyd);
        Button butOpmlImport = root.findViewById(R.id.butOpmlImport);
        Button butConfirm = root.findViewById(R.id.butConfirm);
        Button butSingleFileAdd = root.findViewById(R.id.butSingleFileAdd);

        final MainActivity activity = (MainActivity) getActivity();
        activity.getSupportActionBar().setTitle(R.string.add_feed_label);

        butSearchITunes.setOnClickListener(v -> activity.loadChildFragment(new ItunesSearchFragment()));

        butBrowserGpoddernet.setOnClickListener(v -> activity.loadChildFragment(new GpodnetMainFragment()));

        butSearchFyyd.setOnClickListener(v -> activity.loadChildFragment(new FyydSearchFragment()));

        butOpmlImport.setOnClickListener(v -> startActivity(new Intent(getActivity(),
                OpmlImportFromPathActivity.class)));

        butSingleFileAdd.setOnClickListener(v -> startActivity(new Intent(getActivity(),
                SingleFileAddActivity.class)));

        butConfirm.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), OnlineFeedViewActivity.class);
            intent.putExtra(OnlineFeedViewActivity.ARG_FEEDURL, etxtFeedurl.getText().toString());
            intent.putExtra(OnlineFeedViewActivity.ARG_TITLE, getString(R.string.add_feed_label));
            startActivity(intent);
        });

        return root;
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
