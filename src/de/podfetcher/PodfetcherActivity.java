package de.podfetcher;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.podfetcher.feed.*;
import de.podfetcher.storage.DownloadRequester;
import greendroid.app.GDListActivity;
import greendroid.widget.ItemAdapter;
import greendroid.widget.item.TextItem;
import greendroid.widget.item.Item;
import greendroid.widget.ActionBar;
import greendroid.widget.ActionBar.Type;
import greendroid.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.os.Bundle;
import android.content.Intent;

public class PodfetcherActivity extends GDListActivity {

	public PodfetcherActivity() {
		super(ActionBar.Type.Normal);
	}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


		// Add navigation menu
		ItemAdapter adapter = new ItemAdapter(this);
		adapter.add(createListItem(R.string.feeds_label, FeedlistActivity.class));
		adapter.add(new TextItem("Settings"));

		setListAdapter(adapter);

         
    }

	private TextItem createListItem(int id, Class<?> _class) {
		final TextItem item = new TextItem(getString(id));
		item.setTag(_class);
		return item;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final TextItem item = (TextItem) l.getAdapter().getItem(position);
		Intent intent = new Intent(PodfetcherActivity.this, (Class<?>) item.getTag());
		intent.putExtra(ActionBarActivity.GD_ACTION_BAR_TITLE, item.text);
		startActivity(intent);
	}
}
