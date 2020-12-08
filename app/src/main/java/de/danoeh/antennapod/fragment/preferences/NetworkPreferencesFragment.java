package de.danoeh.antennapod.fragment.preferences;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceFragmentCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

import java.util.Calendar;
import java.util.GregorianCalendar;
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

        Activity activity= ((PreferenceActivity) getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpdateIntervalText();
        setParallelDownloadsText(UserPreferences.getParallelDownloads());
    }

    private void setupNetworkScreen()
    {
        findPreference(PREF_SCREEN_AUTODL).setOnPreferenceClickListener(preference -> {
            ((PreferenceActivity) getActivity()).openScreen(R.xml.preferences_autodownload);
            return true;
        });
        findPreference(UserPreferences.PREF_UPDATE_INTERVAL)
                .setOnPreferenceClickListener(preference -> {
                    ViewDialog alert = new ViewDialog();
                      alert.showDialog(getActivity(), "Error de conexión al servidor");
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

    private void setUpdateIntervalText()
    {
        Context context = getActivity().getApplicationContext();
        String val;
        long interval = UserPreferences.getUpdateInterval();
        if(interval > 0) {
            int hours = (int) TimeUnit.MILLISECONDS.toHours(interval);
            String hoursStr = context.getResources().getQuantityString(R.plurals.time_hours_quantified, hours, hours);
            val = String.format(context.getString(R.string.pref_autoUpdateIntervallOrTime_every), hoursStr);
        } else {
            int[] timeOfDay = UserPreferences.getUpdateTimeOfDay();
            if(timeOfDay.length == 2) {
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

    private void showUpdateIntervalTimePreferencesDialog()
    {
        final Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.pref_autoUpdateIntervallOrTime_title);
        builder.setMessage(R.string.pref_autoUpdateIntervallOrTime_message);
        builder.setPositiveButton(R.string.pref_autoUpdateIntervallOrTime_Interval, (dialog, which) ->
        {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
            builder1.setTitle(context.getString(R.string.pref_autoUpdateIntervallOrTime_Interval));

            //Get default Interval Values from Array
            final String[] values = context.getResources().getStringArray(R.array.update_intervall_values);

            //Get from Preferences Array in String
            final String[] entries = getUpdateIntervalEntries(values);
            //Get Interval Value from User Preferences
            long currInterval = UserPreferences.getUpdateInterval();
            int checkedItem = -1;
            if(currInterval > 0)
            {
                String currIntervalStr = String.valueOf(TimeUnit.MILLISECONDS.toHours(currInterval));
                checkedItem = ArrayUtils.indexOf(values, currIntervalStr);
            }
            builder1.setSingleChoiceItems(entries, checkedItem, (dialog1, which1) ->
            {
                int hours = Integer.parseInt(values[which1]);
                UserPreferences.setUpdateInterval(hours);
                dialog1.dismiss();
                setUpdateIntervalText();
            });
            builder1.setNegativeButton(context.getString(R.string.cancel_label), null);
            builder1.show();
        });

        builder.setNegativeButton(R.string.pref_autoUpdateIntervallOrTime_TimeOfDay, (dialog, which) ->
        {
            int hourOfDay = 7;
            int minute = 0;
            int[] updateTime = UserPreferences.getUpdateTimeOfDay();
            if (updateTime.length == 2) {
                hourOfDay = updateTime[0];
                minute = updateTime[1];
            }
            TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                    (view, selectedHourOfDay, selectedMinute) -> {
                        if (view.getTag() == null) { // onTimeSet() may get called twice!
                            UserPreferences.setUpdateTimeOfDay(selectedHourOfDay, selectedMinute);
                            setUpdateIntervalText();

                        }
                    }, hourOfDay, minute, DateFormat.is24HourFormat(context));

            timePickerDialog.setTitle(context.getString(R.string.pref_autoUpdateIntervallOrTime_TimeOfDay));
            timePickerDialog.show();
        });


        builder.setNeutralButton(R.string.pref_autoUpdateIntervallOrTime_Disable, (dialog, which) -> {
            UserPreferences.disableAutoUpdate(context);
            setUpdateIntervalText();
        });

        AlertDialog.Builder builderAct = new AlertDialog.Builder(context);
        builderAct.setTitle(R.string.pref_autoUpdateIntervallOrTime_titleAct);
        builderAct.setMessage(R.string.pref_autoUpdateIntervallOrTime_sumAct);





        builder.show();
    }






    private String[] getUpdateIntervalEntries(final String[] values)
    {
        final Resources res = getActivity().getResources();
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            Integer v = Integer.parseInt(values[x]);
            switch (v) {
                case 0:
                    entries[x] = res.getString(R.string.pref_update_interval_hours_manual);
                    break;
                case 1:
                    entries[x] = v + " " + res.getString(R.string.pref_update_interval_hours_singular);
                    break;
                default:
                    entries[x] = v + " " + res.getString(R.string.pref_update_interval_hours_plural);
                    break;

            }
        }
        return entries;
    }


    public class ViewDialog {


        final Context context = getActivity();
        int string_spinnercase;

        public void showDialog(Activity activity, String msg){
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.update_refresh_time_dialog);
            Spinner spinner = (Spinner) dialog.findViewById(R.id.spinner);

            RadioButton radioButton_interval = (RadioButton) dialog.findViewById(R.id.intervalRadioButton);
            RadioButton radioButtonTime = (RadioButton) dialog.findViewById(R.id.timeRadioButton);
            RadioButton radioButton_disable = (RadioButton) dialog.findViewById(R.id.disableButton);
            RadioGroup radioGroupOne = (RadioGroup) dialog.findViewById(R.id.genderRadioGroup);
            TimePicker timePicker = (TimePicker) dialog.findViewById(R.id.timePicker);
            Button button_accept = (Button) dialog.findViewById(R.id.button_accept);
            Button button_cancel = (Button) dialog.findViewById(R.id.button_cancel);



            radioGroupOne.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i)
                {
                    switch (i)

                    {
                        case R.id.intervalRadioButton:
                            // ->Spinner
                            //Get default Interval Values from Array
                            final String[] values = context.getResources().getStringArray(R.array.update_intervall_values);

                            //Get from Preferences Array in String
                            final String[] entries = getUpdateIntervalEntries(values);
                            //Get Interval Value from User Preferences
                            long currInterval = UserPreferences.getUpdateInterval();
                            int checkedItem = -1;
                            if(currInterval > 0)
                            {
                                String currIntervalStr = String.valueOf(TimeUnit.MILLISECONDS.toHours(currInterval));
                                checkedItem = ArrayUtils.indexOf(values, currIntervalStr);
                            }
                            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, entries); //selected item will look like a spinner set from XML
                            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner.setAdapter(spinnerArrayAdapter);
                            spinner.setSelection(checkedItem);
                            spinner.setVisibility(View.VISIBLE);
                            timePicker.setVisibility(View.GONE);
                             string_spinnercase= R.string.pref_autoUpdateIntervallOrTime_CheckStatusInterval;

                            break;
                        case R.id.timeRadioButton:
                            spinner.setVisibility(View.GONE);
                            timePicker.setVisibility(View.VISIBLE);
                            string_spinnercase= R.string.pref_autoUpdateIntervallOrTime_CheckStatusTime;
                            int hourOfDay = 7;
                            int minute = 0;
                            int[] updateTime = UserPreferences.getUpdateTimeOfDay();
                            if (updateTime.length == 2) {
                                hourOfDay = updateTime[0];
                                minute = updateTime[1];
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            {
                                timePicker.setHour(hourOfDay);
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            {
                                timePicker.setMinute(minute);
                            }
                            break;
                        case R.id.disableButton:
                            spinner.setVisibility(View.GONE);
                            timePicker.setVisibility(View.GONE);
                            string_spinnercase= R.string.pref_autoUpdateIntervallOrTime_CheckStatusNever;

                            break;
                        default:

                    }
                }
            });
            radioButton_interval.setChecked(true);


            button_accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (string_spinnercase)
                    {
                        case R.string.pref_autoUpdateIntervallOrTime_CheckStatusInterval:
                            String text = spinner.getSelectedItem().toString();
                            String[] parts = text.split(" ");
                            int hours = Integer.parseInt(parts[0]);
                            UserPreferences.setUpdateInterval(hours);
                            setUpdateIntervalText();
                            dialog.cancel();
                            break;
                        case R.string.pref_autoUpdateIntervallOrTime_CheckStatusTime:




                            int hour = 0;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                hour = timePicker.getHour();
                            }
                            int minutx = 0;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                minutx = timePicker.getMinute();
                            }

                            UserPreferences.setUpdateTimeOfDay(hour, minutx);
                            setUpdateIntervalText();

                            Log.e("Accept Time "," hour: " + hour + " minute: " + minutx);



                            Log.e("Switch Status", " " + R.string.pref_autoUpdateIntervallOrTime_CheckStatusTime);

                            dialog.cancel();

                            break;
                        case R.string.pref_autoUpdateIntervallOrTime_CheckStatusNever:
                            Log.e("Switch Status", " " + R.string.pref_autoUpdateIntervallOrTime_CheckStatusNever);

                            UserPreferences.disableAutoUpdate(context);
                            setUpdateIntervalText();
                            dialog.cancel();
                            break;



                        default:
                            throw new IllegalStateException("Unexpected value: " + string_spinnercase);
                    }
                    dialog.cancel();
                }
            });


            button_cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.cancel();
                }
            });

            dialog.show();

        }
    }
}
