package de.danoeh.antennapod.dialog;

import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public abstract class TimeDialog extends Dialog {
	private int leftButtonTextId;
	private int titleTextId;

	private EditText etxtTime;
	private Spinner spTimeUnit;
	private Button butConfirm;
	private Button butCancel;

	private String[] spinnerContent = { "min", "h" };
	private TimeUnit[] units = { TimeUnit.MINUTES, TimeUnit.HOURS };

	public TimeDialog(Context context, int TitleTextId, int leftButtonTextId) {
		super(context);
		this.leftButtonTextId = leftButtonTextId;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.time_dialog);
		etxtTime = (EditText) findViewById(R.id.etxtTime);
		spTimeUnit = (Spinner) findViewById(R.id.spTimeUnit);
		butConfirm = (Button) findViewById(R.id.butConfirm);
		butCancel = (Button) findViewById(R.id.butCancel);
		butConfirm.setText(leftButtonTextId);
		butCancel.setText(R.string.cancel_label);
		setTitle(titleTextId);
		spTimeUnit.setAdapter(new ArrayAdapter<String>(this.getContext(), 0,
				spinnerContent));
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
