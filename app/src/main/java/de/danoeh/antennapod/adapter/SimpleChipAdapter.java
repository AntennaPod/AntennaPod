package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import de.danoeh.antennapod.R;

import java.util.List;

public abstract class SimpleChipAdapter extends RecyclerView.Adapter<SimpleChipAdapter.ViewHolder> {
    private final Context context;

    public SimpleChipAdapter(Context context) {
        this.context = context;
        setHasStableIds(true);
    }

    protected abstract List<String> getChips();

    protected abstract void onRemoveClicked(int position);

    @Override
    @NonNull
    public SimpleChipAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Chip chip = new Chip(context);
        chip.setCloseIconVisible(true);
        chip.setCloseIconResource(R.drawable.ic_delete);
        return new SimpleChipAdapter.ViewHolder(chip);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleChipAdapter.ViewHolder holder, int position) {
        holder.chip.setText(getChips().get(position));
        holder.chip.setOnCloseIconClickListener(v -> onRemoveClicked(position));
    }

    @Override
    public int getItemCount() {
        return getChips().size();
    }

    @Override
    public long getItemId(int position) {
        return getChips().get(position).hashCode();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Chip chip;

        ViewHolder(Chip itemView) {
            super(itemView);
            chip = itemView;
        }
    }
}