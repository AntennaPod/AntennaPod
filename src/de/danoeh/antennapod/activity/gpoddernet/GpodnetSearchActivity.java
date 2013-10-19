package de.danoeh.antennapod.activity.gpoddernet;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.gpodnet.SearchListFragment;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by daniel on 23.08.13.
 */
public class GpodnetSearchActivity extends GpodnetActivity {

    private SearchListFragment searchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.gpodnet_search);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (StringUtils.equals(intent.getAction(), Intent.ACTION_SEARCH)) {
            handleSearchRequest(intent.getStringExtra(SearchManager.QUERY));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    private void handleSearchRequest(String query) {
        getSupportActionBar().setSubtitle(getString(R.string.search_term_label) + query);
        if (searchFragment == null) {
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();
            searchFragment = SearchListFragment.newInstance(query);
            transaction.replace(R.id.searchListFragment, searchFragment);
            transaction.commit();
        } else {
            searchFragment.changeQuery(query);
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
