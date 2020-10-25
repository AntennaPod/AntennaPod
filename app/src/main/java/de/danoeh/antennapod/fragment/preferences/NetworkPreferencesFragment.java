package de.danoeh.antennapod.fragment.preferences;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceFragmentCompat;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.dialog.ProxyDialog;

import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NetworkPreferencesFragment extends PreferenceFragmentCompat {
    private static final String PREF_SCREEN_AUTODL = "prefAutoDownloadSettings";
    private static final String PREF_PROXY = "prefProxy";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_network);
        setupNetworkScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.network_pref);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpdateIntervalText();
        setParallelDownloadsText(UserPreferences.getParallelDownloads());
    }

    private void setupNetworkScreen() {
        findPreference(PREF_SCREEN_AUTODL).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_autodownload);
            return true;
        });
        findPreference(UserPreferences.PREF_UPDATE_INTERVAL)
                .setOnPreferenceClickListener(preference -> {
                    showUpdateIntervalTimePreferencesDialog();
                    return true;
                });
        findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS)
                .setOnPreferenceChangeListener(
                        (preference, o) -> {
                            if (o instanceof Integer) {
                                setParallelDownloadsText((Integer) o);
                            }
                            return true;
                        }
                );
        // validate and set correct value: number of downloads between 1 and 50 (inclusive)
        findPreference(PREF_PROXY).setOnPreferenceClickListener(preference -> {
            ProxyDialog dialog = new ProxyDialog(getActivity());
            dialog.show();
            return true;
        });
    }

    private void setUpdateIntervalText() {
        Context context = getActivity().getApplicationContext();
        String val;
        long interval = UserPreferences.getUpdateInterval();
        if (interval > 0) {
            int hours = (int) TimeUnit.MILLISECONDS.toHours(interval);
            String hoursStr = context.getResources().getQuantityString(R.plurals.time_hours_quantified, hours, hours);
            val = String.format(context.getString(R.string.pref_autoUpdateIntervallOrTime_every), hoursStr);
        } else {
            int[] timeOfDay = UserPreferences.getUpdateTimeOfDay();
            if (timeOfDay.length == 2) {
                Calendar cal = new GregorianCalendar();
                cal.set(Calendar.HOUR_OF_DAY, timeOfDay[0]);
                cal.set(Calendar.MINUTE, timeOfDay[1]);
                String timeOfDayStr = DateFormat.getTimeFormat(context).format(cal.getTime());
                val = String.format(context.getString(R.string.pref_autoUpdateIntervallOrTime_at),
                        timeOfDayStr);
            } else {
                val = context.getString(R.string.pref_smart_mark_as_played_disabled);  // TODO: Is this a bug? Otherwise document why is this related to smart mark???
            }
        }
        String summary = context.getString(R.string.pref_autoUpdateIntervallOrTime_sum) + "\n"
                + String.format(context.getString(R.string.pref_current_value), val);
        findPreference(UserPreferences.PREF_UPDATE_INTERVAL).setSummary(summary);
    }

    private void setParallelDownloadsText(int downloads) {
        final Resources res = getActivity().getResources();

        String s = String.format(Locale.getDefault(), "%d%s",
                downloads, res.getString(R.string.parallel_downloads_suffix));
        findPreference(UserPreferences.PREF_PARALLEL_DOWNLOADS).setSummary(s);
    }

    private void showUpdateIntervalTimePreferencesDialog() {
        final Context context = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        builder.setTitle(R.string.pref_autoUpdateIntervallOrTime_title);

        CharSequence[] options = new CharSequence[]{
                context.getText(R.string.pref_autoUpdateIntervallOrTime_Interval),
                context.getText(R.string.pref_autoUpdateIntervallOrTime_TimeOfDay),
                context.getText(R.string.pref_autoUpdateIntervallOrTime_Disable)
        };

        int selected;
        if (UserPreferences.getUpdateInterval() != 0)
            selected = 0;
        else if (UserPreferences.getUpdateTimeOfDay().length != 0)
            selected = 1;
        else
            selected = 2;

        View picker = getPicker(selected, context);

        RadioGroup radioButtons = new RadioGroup(context);
        for (CharSequence option : options) {
            RadioButton b = new RadioButton(context);
            b.setText(option);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int selected = radioButtons.indexOfChild(view);
                    View newPicker = getPicker(selected, context);
                    content.removeViewAt(1);
                    content.addView(newPicker);
                }
            });
            radioButtons.addView(b);
        }
        content.addView(radioButtons);
        ((RadioButton) radioButtons.getChildAt(selected)).setChecked(true);

        content.addView(picker);
        builder.setView(content);
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> builder.create().cancel());
        builder.setPositiveButton("Save", (dialogInterface, i) -> {
            int id = radioButtons.getCheckedRadioButtonId();
            int selected1 = radioButtons.indexOfChild(radioButtons.findViewById(id));
            if (selected1 == 0) {
                int interval = Integer.parseInt(((Spinner) content.getChildAt(1)).getSelectedItem().toString());
                UserPreferences.setUpdateInterval(interval);
                setUpdateIntervalText();
            } else if (selected1 == 1) {
                int hour = ((TimePicker) content.getChildAt(1)).getCurrentHour();
                int minute = ((TimePicker) content.getChildAt(1)).getCurrentMinute();
                UserPreferences.setUpdateTimeOfDay(hour, minute);
            } else {
                UserPreferences.disableAutoUpdate(context);
            }
            setUpdateIntervalText();
        });
        builder.show();
    }

    private View getPicker(int selected, Context context) {
        switch (selected) {
            case 0:
                Spinner intervalPicker = new Spinner(context);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.update_intervall_values, android.R.layout.simple_spinner_item);
                intervalPicker.setAdapter(adapter);
                int current = (int) TimeUnit.MILLISECONDS.toHours(UserPreferences.getUpdateInterval());
                if (current == 0) current = 4; // default value
                Log.println(Log.INFO, "Current Intervall", Integer.toString(current));
                int position = adapter.getPosition(Long.toString(current));
                intervalPicker.setSelection(position);
                return intervalPicker;
            case 1:
                TimePicker timePicker = new TimePicker(context);
                int[] time = UserPreferences.getUpdateTimeOfDay();
                if (time.length == 2) {
                    timePicker.setCurrentHour(time[0]);
                    timePicker.setCurrentMinute(time[1]);
                } else {
                    timePicker.setCurrentHour(7);
                    timePicker.setCurrentMinute(0);
                }
                return timePicker;
            default:
                TextView disabledText = new TextView(context);
                disabledText.setText("Automatic updates disabled. ");
                return disabledText;
        }
    }
}
