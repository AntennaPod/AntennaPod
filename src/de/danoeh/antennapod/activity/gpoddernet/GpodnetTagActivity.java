package de.danoeh.antennapod.activity.gpoddernet;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.gpodnet.PodcastListFragment;
import de.danoeh.antennapod.gpoddernet.GpodnetService;
import de.danoeh.antennapod.gpoddernet.GpodnetServiceException;
import de.danoeh.antennapod.gpoddernet.model.GpodnetPodcast;
import de.danoeh.antennapod.gpoddernet.model.GpodnetTag;

import java.util.List;

/**
 * Created by daniel on 23.08.13.
 */
public class GpodnetTagActivity extends GpodnetActivity{

    private static final int PODCAST_COUNT = 50;
    public static final String ARG_TAGNAME = "tagname";

    private GpodnetTag tag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.gpodnet_tag_activity);

        if (!getIntent().hasExtra(ARG_TAGNAME)) {
            throw new IllegalArgumentException("No tagname argument");
        }
        tag = new GpodnetTag(getIntent().getStringExtra(ARG_TAGNAME));
        getSupportActionBar().setTitle(tag.getName());

        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        Fragment taglistFragment = new TaglistFragment();
        transaction.replace(R.id.taglistFragment, taglistFragment);
        transaction.commit();
    }

    private class TaglistFragment extends PodcastListFragment {

        @Override
        protected List<GpodnetPodcast> loadPodcastData(GpodnetService service) throws GpodnetServiceException {
            return service.getPodcastsForTag(tag, PODCAST_COUNT);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
