package de.danoeh.antennapod.activity.gpoddernet;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.gpodnet.PodcastTopListFragment;

/**
 * Created by daniel on 22.08.13.
 */
public class GpodnetMainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gpodnet_main);
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        PodcastTopListFragment topListFragment = new PodcastTopListFragment();
        transaction.replace(R.id.toplist_fragment, topListFragment);
        transaction.commit();
    }
}
