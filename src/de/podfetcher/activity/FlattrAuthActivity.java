package de.podfetcher.activity;

import org.shredzone.flattr4j.exception.FlattrException;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.podfetcher.R;
import de.podfetcher.util.FlattrUtils;

/** Guides the user through the authentication process */
public class FlattrAuthActivity extends SherlockActivity {
	private static final String TAG = "FlattrAuthActivity";

	private TextView txtvExplanation;
	private Button butAuthenticate;
	private Button butReturn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Activity created");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.flattr_auth);
		txtvExplanation = (TextView) findViewById(R.id.txtvExplanation);
		butAuthenticate = (Button) findViewById(R.id.but_authenticate);
		butReturn = (Button) findViewById(R.id.but_return_home);

		butReturn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(FlattrAuthActivity.this,
						PodfetcherActivity.class));
			}
		});
		
		butAuthenticate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					FlattrUtils.startAuthProcess(FlattrAuthActivity.this);
				} catch (FlattrException e) {
					e.printStackTrace();
				}
			}	
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Activity resumed");
		Uri uri = getIntent().getData();
		if (uri != null) {
			Log.d(TAG, "Received uri");
			try {
				if (FlattrUtils.handleCallback(uri) != null) {
					handleAuthenticationSuccess();
					Log.d(TAG, "Authentication seemed to be successful");
				}
			} catch (FlattrException e) {
				e.printStackTrace();
			} 
		}
	}

	private void handleAuthenticationSuccess() {
		txtvExplanation.setText(R.string.flattr_auth_success);
		butAuthenticate.setEnabled(false);
		butReturn.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		default:
			return false;
		}
		return true;
	}

}
