package de.podfetcher;

import greendroid.app.GDListActivity;
import android.os.Bundle;
import android.view.View;
import greendroid.widget.ActionBarItem.Type;

public class FeedlistActivity extends GDListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addActionBarItem(Type.Add, R.id.action_bar_add);
		addActionBarItem(Type.Refresh, R.id.action_bar_refresh);

	}
}
