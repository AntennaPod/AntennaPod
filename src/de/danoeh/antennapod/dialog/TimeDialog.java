package de.danoeh.antennapod.dialog;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public abstract class TimeDialog extends Dialog {

	private static final int DEFAULT_SPINNER_POSITION = 1;
	
	private EditText etxtTime;
	private Spinner spTimeUnit;
	private Button butConfirm;
	private Button butCancel;

	private String[] spinnerContent = { "s", "min", "h" };
	private TimeUnit[] units = { TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS };

	public TimeDialog(Context context, int titleTextId, int leftButtonTextId) {
		super(context);
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
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_item,
				spinnerContent);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
				dismiss();
				onTimeEntered(readTimeMillis());
			}
		});

	}

	public abstract void onTimeEntered(long millis);

	private long readTimeMillis() {
		TimeUnit selectedUnit = units[spTimeUnit.getSelectedItemPosition()];
		long value = Long.valueOf(etxtTime.getText().toString());
		return selectedUnit.toMillis(value);
	}

}
