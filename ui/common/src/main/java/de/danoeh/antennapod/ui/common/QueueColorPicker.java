package de.danoeh.antennapod.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.ui.common.databinding.ColorPickerItemBinding;

/**
 * Dialog for selecting queue color from Material Design palette.
 *
 * <p>Displays a 14-color palette from Material Design colors in a 4-column grid.
 * Users can select a color or choose "No Color" to use the default color.
 *
 * <p>Color Palette (Material Design 500 shades):
 * - Red, Pink, Purple, Deep Purple
 * - Indigo, Blue, Light Blue, Cyan
 * - Teal, Green, Light Green, Lime
 * - Amber, Orange
 *
 * <p>Usage:
 * <pre>
 * QueueColorPicker picker = QueueColorPicker.newInstance(
 *     currentColor,
 *     selectedColor -> {
 *         // Handle color selection
 *         queue.setColor(selectedColor);
 *     }
 * );
 * picker.show(getSupportFragmentManager(), "color_picker");
 * </pre>
 */
public class QueueColorPicker extends DialogFragment {

    private static final String ARG_CURRENT_COLOR = "current_color";

    /**
     * Material Design color palette (500 shades).
     */
    private static final int[] COLOR_PALETTE = {
            Color.parseColor("#F44336"),  // Red
            Color.parseColor("#E91E63"),  // Pink
            Color.parseColor("#9C27B0"),  // Purple
            Color.parseColor("#673AB7"),  // Deep Purple
            Color.parseColor("#3F51B5"),  // Indigo
            Color.parseColor("#2196F3"),  // Blue
            Color.parseColor("#03A9F4"),  // Light Blue
            Color.parseColor("#00BCD4"),  // Cyan
            Color.parseColor("#009688"),  // Teal
            Color.parseColor("#4CAF50"),  // Green
            Color.parseColor("#8BC34A"),  // Light Green
            Color.parseColor("#CDDC39"),  // Lime
            Color.parseColor("#FFC107"),  // Amber
            Color.parseColor("#FF9800"),  // Orange
    };

    private static final int NO_COLOR = 0; // Represents "use default color"

    private OnColorSelectedListener colorSelectedListener;
    private int currentColor;

    /**
     * Interface for color selection events.
     */
    public interface OnColorSelectedListener {
        /**
         * Called when a color is selected.
         *
         * @param color Selected ARGB color, or 0 for "no color" (use default)
         */
        void onColorSelected(@ColorInt int color);
    }

    /**
     * Creates a new QueueColorPicker instance.
     *
     * @param currentColor         Currently selected color (0 for no color)
     * @param colorSelectedListener Listener for color selection
     * @return New QueueColorPicker instance
     */
    public static QueueColorPicker newInstance(@ColorInt int currentColor,
                                                @NonNull OnColorSelectedListener colorSelectedListener) {
        QueueColorPicker picker = new QueueColorPicker();
        Bundle args = new Bundle();
        args.putInt(ARG_CURRENT_COLOR, currentColor);
        picker.setArguments(args);
        picker.colorSelectedListener = colorSelectedListener;
        return picker;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            currentColor = getArguments().getInt(ARG_CURRENT_COLOR, NO_COLOR);
        }

        // Create RecyclerView for color grid
        RecyclerView colorGrid = new RecyclerView(requireContext());
        colorGrid.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        colorGrid.setPadding(16, 16, 16, 16);

        // Add "No Color" option at the beginning
        ColorAdapter adapter = new ColorAdapter(requireContext(), currentColor);
        colorGrid.setAdapter(adapter);

        // Build dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Select Queue Color");
        builder.setView(colorGrid);
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    /**
     * Adapter for color grid display.
     */
    private class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

        private final Context context;
        private final int selectedColor;
        private final int[] colors;

        ColorAdapter(@NonNull Context context, @ColorInt int selectedColor) {
            this.context = context;
            this.selectedColor = selectedColor;
            // Add "No Color" option at index 0
            this.colors = new int[COLOR_PALETTE.length + 1];
            this.colors[0] = NO_COLOR;
            System.arraycopy(COLOR_PALETTE, 0, this.colors, 1, COLOR_PALETTE.length);
        }

        @NonNull
        @Override
        public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ColorPickerItemBinding binding = ColorPickerItemBinding.inflate(LayoutInflater.from(context),
                    parent, false);
            return new ColorViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
            int color = colors[position];
            holder.bind(color, color == selectedColor);
        }

        @Override
        public int getItemCount() {
            return colors.length;
        }

        /**
         * ViewHolder for color items.
         */
        class ColorViewHolder extends RecyclerView.ViewHolder {
            private final ColorPickerItemBinding binding;

            ColorViewHolder(@NonNull ColorPickerItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(@ColorInt int color, boolean isSelected) {
                // Set color circle background
                if (color == NO_COLOR) {
                    // "No Color" option - show pattern or crossed circle
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.OVAL);
                    drawable.setStroke(3, Color.GRAY);
                    drawable.setColor(Color.WHITE);
                    binding.colorCircle.setBackground(drawable);
                } else {
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.OVAL);
                    drawable.setColor(color);
                    binding.colorCircle.setBackground(drawable);
                }

                // Show checkmark if selected
                binding.checkmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                // Handle click
                binding.getRoot().setOnClickListener(v -> {
                    if (colorSelectedListener != null) {
                        colorSelectedListener.onColorSelected(color);
                    }
                    dismiss();
                });
            }
        }
    }

    /**
     * Gets the Material Design color palette.
     *
     * @return Array of ARGB color values
     */
    @NonNull
    public static int[] getColorPalette() {
        return COLOR_PALETTE.clone();
    }

    /**
     * Gets the "No Color" value (represents default color).
     *
     * @return 0 (transparent/no color)
     */
    @ColorInt
    public static int getNoColor() {
        return NO_COLOR;
    }
}
