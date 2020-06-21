package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItemFilter;
import de.danoeh.antennapod.core.feed.FeedItemFilterGroup;

public abstract class FilterDialog {

    protected FeedItemFilter filter;
    protected Context context;

    public FilterDialog(Context context, FeedItemFilter feedItemFilter) {
        this.context = context;
        this.filter = feedItemFilter;
    }

    public void openDialog() {

        final Set<String> filterValues = new HashSet<>(Arrays.asList(filter.getValues()));
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.filter);

        LayoutInflater inflater = LayoutInflater.from(this.context);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.filter_dialog_layout, null, false);
        builder.setView(layout);

        for (FeedItemFilterGroup.FeedItemEnum item : FeedItemFilterGroup.FeedItemEnum.values()) {

            RelativeLayout row = (RelativeLayout) inflater.inflate(R.layout.filter_dialog_relative_cardview, null);
            RadioButton radioButton1 = row.findViewById(R.id.filter_dialog_radioButton1);
            RadioButton radioButton2 = row.findViewById(R.id.filter_dialog_radioButton2);
            RadioButton radioButton3 = row.findViewById(R.id.filter_dialog_radioButton3);
            radioButton1.setText(item.values[1].displayName);
            radioButton1.setTextColor(Color.BLACK);
            radioButton2.setText(item.values[0].displayName);
            radioButton2.setTextColor(Color.BLACK);

            Iterator<String> filterIterator = filterValues.iterator();
            while (filterIterator.hasNext()) {
                String nextItem = filterIterator.next();
                if (item.values[1].filterId.equals(nextItem)) {
                    item.values[1].setSelected(true);
                    item.values[0].setSelected(false);
                    radioButton1.setBackgroundResource(R.color.accent_light);
                    radioButton2.setBackgroundResource(R.color.master_switch_background_light);
                    radioButton3.setBackgroundResource(R.drawable.filter_dialog_x_on);
                    radioButton1.setSelected(true);
                    radioButton2.setSelected(false);
                    radioButton1.setTextColor(Color.WHITE);
                    radioButton2.setTextColor(Color.BLACK);
                }
                if (item.values[0].filterId.equals(nextItem)) {
                    item.values[0].setSelected(true);
                    item.values[1].setSelected(false);
                    radioButton2.setBackgroundResource(R.color.accent_light);
                    radioButton1.setBackgroundResource(R.color.master_switch_background_light);
                    radioButton3.setBackgroundResource(R.drawable.filter_dialog_x_on);
                    radioButton2.setSelected(true);
                    radioButton1.setSelected(false);
                    radioButton2.setTextColor(Color.WHITE);
                    radioButton1.setTextColor(Color.BLACK);
                }
            }

            radioButton1.setOnClickListener(arg0 -> {
                item.values[1].setSelected(true);
                item.values[0].setSelected(false);
                radioButton1.setBackgroundResource(R.color.accent_light);
                radioButton2.setBackgroundResource(R.color.master_switch_background_light);
                radioButton3.setBackgroundResource(R.drawable.filter_dialog_x_on);
                radioButton2.setSelected(false);
                radioButton2.setTextColor(Color.BLACK);
                radioButton1.setSelected(true);
                radioButton1.setTextColor(Color.WHITE);
            });
            radioButton2.setOnClickListener(arg0 -> {
                item.values[0].setSelected(true);
                item.values[1].setSelected(false);
                radioButton2.setBackgroundResource(R.color.accent_light);
                radioButton1.setBackgroundResource(R.color.master_switch_background_light);
                radioButton3.setBackgroundResource(R.drawable.filter_dialog_x_on);
                radioButton1.setSelected(false);
                radioButton1.setTextColor(Color.BLACK);
                radioButton2.setSelected(true);
                radioButton2.setTextColor(Color.WHITE);
            });
            radioButton3.setOnClickListener(arg0 -> {
                item.values[0].setSelected(false);
                item.values[1].setSelected(false);
                radioButton1.setBackgroundResource(R.color.master_switch_background_light);
                radioButton2.setBackgroundResource(R.color.master_switch_background_light);
                radioButton3.setBackgroundResource(R.drawable.filter_dialog_x_off);
                radioButton2.setTextColor(Color.BLACK);
                radioButton2.setSelected(false);
                radioButton1.setTextColor(Color.BLACK);
                radioButton1.setSelected(false);
            });
            layout.addView(row);
        }


        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            filterValues.clear();
            for (FeedItemFilterGroup.FeedItemEnum item : FeedItemFilterGroup.FeedItemEnum.values()) {
                for (int i = 0; i < item.values.length; i++) {
                    if (item.values[i].getSelected()) {
                        filterValues.add(item.values[i].filterId);
                    }
                }
            }
            updateFilter(filterValues);
        });

        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    protected abstract void updateFilter(Set<String> filterValues);
}
