package de.danoeh.antennapod.ui.transcript;

import android.widget.Space;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.databinding.TranscriptItemBinding;

public class TranscriptViewholder extends RecyclerView.ViewHolder {
    public final TextView viewTimecode;
    public final TextView viewContent;
    public final TextView spaceLeft;
    public final TextView spaceRight;

    public TranscriptViewholder(TranscriptItemBinding binding) {
        super(binding.getRoot());
        viewTimecode = binding.speaker;
        viewContent = binding.content;
        spaceLeft = binding.spacerLeft;
        spaceRight = binding.spacerRight;
    }

    @Override
    public String toString() {
        return super.toString() + " '" + viewContent.getText() + "'";
    }
}