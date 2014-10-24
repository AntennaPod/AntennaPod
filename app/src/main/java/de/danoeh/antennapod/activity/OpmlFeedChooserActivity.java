package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.opml.OpmlElement;
import de.danoeh.antennapod.core.preferences.UserPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the feeds that the OPML-Importer has read and lets the user choose
 * which feeds he wants to import.
 */
public class OpmlFeedChooserActivity extends ActionBarActivity {
    private static final String TAG = "OpmlFeedChooserActivity";

    public static final String EXTRA_SELECTED_ITEMS = "de.danoeh.antennapod.selectedItems";

    private Button butConfirm;
    private Button butCancel;
    private ListView feedlist;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.opml_selection);
        butConfirm = (Button) findViewById(R.id.butConfirm);
        butCancel = (Button) findViewById(R.id.butCancel);
        feedlist = (ListView) findViewById(R.id.feedlist);

        feedlist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice,
                getTitleList());

        feedlist.setAdapter(listAdapter);

        butCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        butConfirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                SparseBooleanArray checked = feedlist.getCheckedItemPositions();

                int checkedCount = 0;
                // Get number of checked items
                for (int i = 0; i < checked.size(); i++) {
                    if (checked.valueAt(i)) {
                        checkedCount++;
                    }
                }
                int[] selection = new int[checkedCount];
                for (int i = 0, collected = 0; collected < checkedCount; i++) {
                    if (checked.valueAt(i)) {
                        selection[collected] = checked.keyAt(i);
                        collected++;
                    }
                }
                intent.putExtra(EXTRA_SELECTED_ITEMS, selection);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

    }

    private List<String> getTitleList() {
        List<String> result = new ArrayList<String>();
        if (OpmlImportHolder.getReadElements() != null) {
            for (OpmlElement element : OpmlImportHolder.getReadElements()) {
                result.add(element.getText());
            }

        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, R.id.select_all_item, Menu.NONE,
                R.string.select_all_label),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, R.id.deselect_all_item, Menu.NONE,
                R.string.deselect_all_label),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.select_all_item:
                selectAllItems(true);
                return true;
            case R.id.deselect_all_item:
                selectAllItems(false);
                return true;
            default:
                return false;
        }
    }

    private void selectAllItems(boolean b) {
        for (int i = 0; i < feedlist.getCount(); i++) {
            feedlist.setItemChecked(i, b);
        }
    }

}
