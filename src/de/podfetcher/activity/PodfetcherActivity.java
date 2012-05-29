package de.podfetcher.activity;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.podfetcher.R;
import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;
import de.podfetcher.fragment.FeedlistFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.os.Bundle;
import android.content.Intent;
import android.app.Activity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

    

public class PodfetcherActivity extends SherlockFragmentActivity {
    private static final String TAG = "PodfetcherActivity";
    
	private FeedlistFragment feedlist;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Set up tabs
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);

		Tab tab = actionBar.newTab()
				.setText(getText(R.string.feeds_label).toString())
				.setTabListener(new TabListener<FeedlistFragment>(
							this, getText(R.string.feeds_label).toString(), FeedlistFragment.class));
		
		actionBar.addTab(tab);
		tab = actionBar.newTab()
				.setText("Playlist")
				.setTabListener(new TabListener<FeedlistFragment>(
							this, "Playlist", FeedlistFragment.class));
		actionBar.addTab(tab);
    }


	/** TabListener for navigating between the main lists. */
	private class TabListener<T extends Fragment> implements ActionBar.TabListener {

		private final Activity activity;
		private final String tag;
		private final Class<T> fClass;
		private Fragment fragment;
		
		public TabListener(Activity activity, String tag, Class<T> fClass) {
			this.activity = activity;
			this.tag = tag;
			this.fClass = fClass;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			if (fragment == null) {
				fragment = Fragment.instantiate(activity, fClass.getName());
				ft.replace(R.id.main_fragment, fragment);
			} else {
				ft.attach(fragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (fragment != null) {
				ft.detach(fragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// Do nothing	
		}
	}
}
