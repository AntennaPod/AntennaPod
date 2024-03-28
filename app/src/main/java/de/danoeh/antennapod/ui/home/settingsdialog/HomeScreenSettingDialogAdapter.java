package de.danoeh.antennapod.ui.home.settingsdialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;

class HomeScreenSettingDialogAdapter extends RecyclerView.Adapter<HomeScreenSettingDialogAdapter.ViewHolder> implements TouchCallbackHelperAdapter {
    private final List<String> sectionLabels;
    @Nullable private Consumer<ViewHolder> dragListener;

    public HomeScreenSettingDialogAdapter(@NonNull Context context)
    {
        this.sectionLabels = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.home_section_titles)));
    }

    public void setDragListener(@Nullable Consumer<ViewHolder> dragListener){
        this.dragListener = dragListener;
    }

    public List<String> getOrderedSectionLabels(Context context)
    {

        return Collections.unmodifiableList(sectionLabels);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View entryView = inflater.inflate(de.danoeh.antennapod.ui.preferences.R.layout.choose_home_screen_order_dialog_entry, parent, false);
        return new HomeScreenSettingDialogAdapter.ViewHolder(entryView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String label = sectionLabels.get(position);
        holder.name.setText(label);
        holder.dragger.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                if(dragListener != null)
                    dragListener.accept(holder);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return sectionLabels.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(sectionLabels, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(sectionLabels, i, i - 1);
            }
        }

        notifyItemMoved(fromPosition, toPosition);
        return false;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final ImageView dragger;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(de.danoeh.antennapod.ui.preferences.R.id.home_screen_section_name);
            dragger = itemView.findViewById(de.danoeh.antennapod.ui.preferences.R.id.home_screen_section_drag);
        }
    }
}
