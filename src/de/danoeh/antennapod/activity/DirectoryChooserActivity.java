package de.danoeh.antennapod.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import de.danoeh.antennapod.PodcastApp;
import de.danoeh.antennapod.R;

/** Let's the user choose a directory on the storage device. */
public class DirectoryChooserActivity extends SherlockActivity {
	private static final String TAG = "DirectoryChooserActivity";

	private Button butConfirm;
	private Button butCancel;
	private Button butNavUp;
	private TextView txtvSelectedFolder;
	private ListView listDirectories;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(PodcastApp.getThemeResourceId());
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.directory_chooser);
		butConfirm = (Button) findViewById(R.id.butConfirm);
		butCancel = (Button) findViewById(R.id.butCancel);
		butNavUp = (Button) findViewById(R.id.butNavUp);
		txtvSelectedFolder = (TextView) findViewById(R.id.txtvSelectedFolder);
		listDirectories = (ListView) findViewById(R.id.directory_list);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.directory_chooser, menu);
		return true;
	}
	
	
	
}
