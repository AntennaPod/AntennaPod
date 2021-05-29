package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.HomeFragment;
import de.danoeh.antennapod.fragment.homesections.InboxSection;
import de.danoeh.antennapod.fragment.homesections.QueueSection;
import de.danoeh.antennapod.fragment.homesections.StatisticsSection;
import de.danoeh.antennapod.fragment.homesections.SubscriptionsSection;
import de.danoeh.antennapod.fragment.homesections.SurpriseSection;
import slush.AdapterAppliedResult;
import slush.Slush;

public class 
HomeSectionsSettingsDialog {
    public static void openHomeDialog(Fragment fragment, DialogInterface.OnClickListener onSettingsChanged) {
        SharedPreferences prefs = fragment.requireActivity().getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        Context context = fragment.requireContext();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.home_label);

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.home_dialog, null, false);
        RecyclerView dialogRecyclerView = layout.findViewById(R.id.dialogRecyclerView);
        Spinner spinner = layout.findViewById(R.id.homeSpinner);
        String[] bottomHalfOptions = new String[]{
                context.getString(R.string.episodes_label),
                context.getString(R.string.inbox_label),
                context.getString(R.string.queue_label)};
        spinner.setAdapter(
                new ArrayAdapter<>(context,
                        android.R.layout.simple_spinner_dropdown_item,
                        bottomHalfOptions));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                prefs.edit().putInt(HomeFragment.PREF_FRAGMENT, i).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner.setSelection(prefs.getInt(HomeFragment.PREF_FRAGMENT, 0));

        ArrayList<HomeFragment.SectionTitle> list = new ArrayList<>(HomeFragment.getSectionsPrefs(fragment));

        //enable only if 2 or less sections are selected
        //spinner.setEnabled(list.stream().filter(s -> s.hidden).count() <= 2);

        AdapterAppliedResult<HomeFragment.SectionTitle> slush = new Slush.SingleType<HomeFragment.SectionTitle>()
                .setItemLayout(R.layout.home_dialog_item)
                .setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false))
                .setItems(list)
                .onBind((view, sectionTitle) -> {
                    TextView title = view.findViewById(R.id.txtvSectionTitle);
                    CheckBox checkBox = view.findViewById(R.id.checkBox);
                    int res;
                    switch (sectionTitle.tag) {
                        case InboxSection.TAG:
                            res = R.string.new_title;
                            break;
                        default:
                        case QueueSection.TAG:
                            res = R.string.continue_title;
                            break;
                        case SubscriptionsSection.TAG:
                            res = R.string.rediscover_title;
                            break;
                        case SurpriseSection.TAG:
                            res = R.string.surprise_title;
                            break;
                        case StatisticsSection.TAG:
                            res = R.string.classics_title;
                            break;
                    }

                    title.setText(context.getString(res));
                    checkBox.setChecked(!sectionTitle.hidden);
                })
                .onItemClick((view, i) -> {
                    CheckBox checkBox = view.findViewById(R.id.checkBox);
                    list.get(i).toggleHidden();
                    checkBox.setChecked(!checkBox.isChecked());
                })
                .into(dialogRecyclerView);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                //int activeSections = list.stream().filter(s -> s.hidden).count();
                //min 1 section active
                //if (activeSections > 1) {
                slush.getItemListEditor().moveItem(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                Collections.swap(list, viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());

                //spinner.setEnabled(activeSections <= 2);
                //}
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

        }).attachToRecyclerView(dialogRecyclerView);

        builder.setView(layout);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            saveSettings(list, fragment);
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNeutralButton(R.string.reset, (dialog, which) -> {
            saveSettings(HomeFragment.defaultSections, fragment);
            onSettingsChanged.onClick(dialog, which);
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    public static void saveSettings(List<HomeFragment.SectionTitle> list, Fragment fragment) {
        SharedPreferences prefs = fragment.requireActivity().getSharedPreferences(HomeFragment.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(HomeFragment.PREF_SECTIONS, encodeSectionSettings(list)).apply();
    }

    public static String encodeSectionSettings(List<HomeFragment.SectionTitle> list) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            HomeFragment.SectionTitle st = list.get(i);
            string.append(st.tag).append(",").append(st.hidden);
            if (i < list.size() - 1) {
                //prevents terminal ;
                string.append(";");
            }
        }
        return string.toString();
    }
}
