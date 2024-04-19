package de.danoeh.antennapod.ui.transcript;

import android.graphics.Typeface;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import de.danoeh.antennapod.databinding.TranscriptItemBinding;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class TranscriptViewholder extends RecyclerView.ViewHolder {
    public final TextView viewTimecode;
    public final TextView viewContent;

    public TranscriptViewholder(TranscriptItemBinding binding) {
        super(binding.getRoot());
        viewTimecode = binding.speaker;
        viewContent = binding.content;
        viewContent.setOnTouchListener((v, event) -> {
            viewContent.setTypeface(null, Typeface.BOLD);
            viewContent.setTextColor(
                    ThemeUtils.getColorFromAttr(v.getContext(), android.R.attr.textColorPrimary)
            );
            return false;
        });
    }

    @Override
    public String toString() {
        return super.toString() + " '" + viewContent.getText() + "'";
    }
}