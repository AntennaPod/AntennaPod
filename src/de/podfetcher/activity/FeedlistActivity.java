package de.podfetcher.activity;

import de.podfetcher.R;
import greendroid.app.GDListActivity;
import android.os.Bundle;
import android.view.View;
import greendroid.widget.ActionBarItem.Type;
import greendroid.widget.ActionBarItem;
import android.content.Intent;

public class FeedlistActivity extends GDListActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addActionBarItem(Type.Add, R.id.action_bar_add);
		addActionBarItem(Type.Refresh, R.id.action_bar_refresh);

	}

	@Override
	public boolean onHandleActionBarItemClick(ActionBarItem item, int position) {
		switch(item.getItemId()) {
			case R.id.action_bar_add:
				startActivity(new Intent(this, AddFeedActivity.class));
				return true;
			default:
				return super.onHandleActionBarItemClick(item, position);
		}
	}
}
