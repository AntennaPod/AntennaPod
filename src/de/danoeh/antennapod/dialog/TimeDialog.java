package de.danoeh.antennapod.dialog;

import java.util.concurrent.TimeUnit;

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

public abstract class TimeDialog extends Dialog {
	private static final String TAG = "TimeDialog";

	private static final int DEFAULT_SPINNER_POSITION = 1;

	private Context context;

	private EditText etxtTime;
	private Spinner spTimeUnit;
	private Button butConfirm;
	private Button butCancel;

	private String[] spinnerContent = { "s", "min", "h" };
	private TimeUnit[] units = { TimeUnit.SECONDS, TimeUnit.MINUTES,
			TimeUnit.HOURS };

	public TimeDialog(Context context, int titleTextId, int leftButtonTextId) {
		super(context);
		this.context = context;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.time_dialog);
		etxtTime = (EditText) findViewById(R.id.etxtTime);
		spTimeUnit = (Spinner) findViewById(R.id.spTimeUnit);
		butConfirm = (Button) findViewById(R.id.butConfirm);
		butCancel = (Button) findViewById(R.id.butCancel);

		butConfirm.setText(R.string.set_sleeptimer_label);
		butCancel.setText(R.string.cancel_label);
		setTitle(R.string.set_sleeptimer_label);
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
				this.getContext(), android.R.layout.simple_spinner_item,
				spinnerContent);
		spinnerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spTimeUnit.setAdapter(spinnerAdapter);
		spTimeUnit.setSelection(DEFAULT_SPINNER_POSITION);
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
					long input = readTimeMillis();
					onTimeEntered(input);
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
		etxtTime.addTextChangedListener(new TextWatcher() {

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
		checkInputLength(etxtTime.getText().length());

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

	public abstract void onTimeEntered(long millis);

	private long readTimeMillis() {
		TimeUnit selectedUnit = units[spTimeUnit.getSelectedItemPosition()];
		long value = Long.valueOf(etxtTime.getText().toString());
		return selectedUnit.toMillis(value);
	}

}
