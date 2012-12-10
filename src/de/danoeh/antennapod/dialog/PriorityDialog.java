package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import de.danoeh.antennapod.AppConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedManager;

public class PriorityDialog extends Dialog {
	private static final String TAG = "TimeDialog";

	private static final int DEFAULT_SPINNER_POSITION = 1;

	private Context context;
	private Feed feed;

	private EditText etxtPriority;
	private Button butConfirm;
	private Button butCancel;

	public PriorityDialog(Context context, Feed feed) {
		super(context);
		this.context = context;
		this.feed = feed;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.priority_dialog);

		etxtPriority = (EditText) findViewById(R.id.etxtPriority);
		butConfirm = (Button) findViewById(R.id.butConfirm);
		butCancel = (Button) findViewById(R.id.butCancel);

		butConfirm.setText(R.string.okay_label);
		butCancel.setText(R.string.cancel_label);
		setTitle(R.string.set_priority_label);
		
		butCancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		butConfirm.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					int priority = readPriority();
					//se the new priority
					feed.setPriority(priority);
					// save the feed
					FeedManager.getInstance().setFeed(context,feed);
					dismiss();
				} catch (NumberFormatException e) {
					e.printStackTrace();
					Toast toast = Toast.makeText(context,
							R.string.time_dialog_invalid_input,
							Toast.LENGTH_LONG);
					toast.show();
				}
			}
		});
		etxtPriority.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				checkInputLength(s.length());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

			}
		});
		if(feed.getPriority()>0) {
			String text=Integer.toString(feed.getPriority());
			etxtPriority.setText(text);
			etxtPriority.setSelection(text.length());
		}
		checkInputLength(etxtPriority.getText().length());

	}

	private void checkInputLength(int length) {
		if (length > 0) {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Length is larger than 0, enabling confirm button");
			butConfirm.setEnabled(true);
		} else {
			if (AppConfig.DEBUG)
				Log.d(TAG, "Length is smaller than 0, disabling confirm button");
			butConfirm.setEnabled(false);
		}
	}

	private int readPriority() {
		int value = Integer.valueOf(etxtPriority.getText().toString());
		return(value);
	}

}
