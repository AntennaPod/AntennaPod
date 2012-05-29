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
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

    

public class PodfetcherActivity extends SherlockFragmentActivity {
    private static final String TAG = "PodfetcherActivity";
    
	private FeedlistFragment feedlist;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		feedlist = new FeedlistFragment();
		fragmentTransaction.replace(R.id.main_fragment, feedlist);
		fragmentTransaction.setTransition(
				FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		fragmentTransaction.commit();

    }
}
