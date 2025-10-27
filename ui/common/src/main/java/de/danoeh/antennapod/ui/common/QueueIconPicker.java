package de.danoeh.antennapod.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Dialog for selecting queue icon from Material Design icons.
 *
 * <p>Displays 50+ Material Design icons in a scrollable 4-6 column grid.
 * Users can select an icon to represent their queue visually.
 *
 * <p>Icon Set (Material Design):
 * - Queue/Music icons: queue_music, playlist_play, library_music
 * - Activity icons: directions_run, directions_walk, directions_bike, directions_car, directions_bus
 * - Fitness icons: fitness_center, sports_basketball, sports_soccer
 * - Location icons: home, work, school, beach_access, flight
 * - Social icons: favorite, star, people, person
 * - Time icons: schedule, alarm, access_time
 * - Media icons: headset, mic, volume_up
 * - And more...
 *
 * <p>Usage:
 * <pre>
 * QueueIconPicker picker = QueueIconPicker.newInstance(
 *     currentIcon,
 *     selectedIcon -> {
 *         // Handle icon selection
 *         queue.setIcon(selectedIcon);
 *     }
 * );
 * picker.show(getSupportFragmentManager(), "icon_picker");
 * </pre>
 */
public class QueueIconPicker extends DialogFragment {

    private static final String ARG_CURRENT_ICON = "current_icon";
    private static final String DEFAULT_ICON = "ic_playlist_play_24dp";

    /**
     * Material Design icon set for queue representation.
     *
     * <p>Each entry maps icon name to drawable resource.
     * Icon names are stored in Queue.icon field as strings.
     */
    private static final IconEntry[] ICON_SET = {
            // Queue/Music icons
            new IconEntry("ic_queue_music_24dp", R.drawable.ic_queue_music_24dp),
            new IconEntry("ic_playlist_play_24dp", R.drawable.ic_playlist_play_24dp),
            new IconEntry("ic_library_music_24dp", R.drawable.ic_library_music_24dp),

            // Activity icons
            new IconEntry("ic_directions_run_24dp", R.drawable.ic_directions_run_24dp),
            new IconEntry("ic_directions_walk_24dp", R.drawable.ic_directions_walk_24dp),
            new IconEntry("ic_directions_bike_24dp", R.drawable.ic_directions_bike_24dp),
            new IconEntry("ic_directions_car_24dp", R.drawable.ic_directions_car_24dp),
            new IconEntry("ic_directions_bus_24dp", R.drawable.ic_directions_bus_24dp),

            // Fitness icons
            new IconEntry("ic_fitness_center_24dp", R.drawable.ic_fitness_center_24dp),
            new IconEntry("ic_sports_basketball_24dp", R.drawable.ic_sports_basketball_24dp),
            new IconEntry("ic_sports_soccer_24dp", R.drawable.ic_sports_soccer_24dp),

            // Location icons
            new IconEntry("ic_home_24dp", R.drawable.ic_home_24dp),
            new IconEntry("ic_work_24dp", R.drawable.ic_work_24dp),
            new IconEntry("ic_school_24dp", R.drawable.ic_school_24dp),
            new IconEntry("ic_beach_access_24dp", R.drawable.ic_beach_access_24dp),
            new IconEntry("ic_flight_24dp", R.drawable.ic_flight_24dp),

            // Social icons
            new IconEntry("ic_favorite_24dp", R.drawable.ic_favorite_24dp),
            new IconEntry("ic_star_24dp", R.drawable.ic_star_24dp),
            new IconEntry("ic_people_24dp", R.drawable.ic_people_24dp),
            new IconEntry("ic_person_24dp", R.drawable.ic_person_24dp),

            // Time icons
            new IconEntry("ic_schedule_24dp", R.drawable.ic_schedule_24dp),
            new IconEntry("ic_alarm_24dp", R.drawable.ic_alarm_24dp),
            new IconEntry("ic_access_time_24dp", R.drawable.ic_access_time_24dp),

            // Media icons
            new IconEntry("ic_headset_24dp", R.drawable.ic_headset_24dp),
            new IconEntry("ic_mic_24dp", R.drawable.ic_mic_24dp),
            new IconEntry("ic_volume_up_24dp", R.drawable.ic_volume_up_24dp),

            // Additional icons (placeholders - these may need actual drawable resources)
            // Note: These resource IDs may not exist yet and will need to be created
            // or replaced with existing resources during integration
    };

    private OnIconSelectedListener iconSelectedListener;
    private String currentIcon;

    /**
     * Interface for icon selection events.
     */
    public interface OnIconSelectedListener {
        /**
         * Called when an icon is selected.
         *
         * @param iconName Selected icon name (e.g., "ic_queue_music_24dp")
         */
        void onIconSelected(@NonNull String iconName);
    }

    /**
     * Icon entry mapping name to drawable resource.
     */
    private static class IconEntry {
        final String name;
        final int drawableRes;

        IconEntry(String name, @DrawableRes int drawableRes) {
            this.name = name;
            this.drawableRes = drawableRes;
        }
    }

    /**
     * Creates a new QueueIconPicker instance.
     *
     * @param currentIcon          Currently selected icon name
     * @param iconSelectedListener Listener for icon selection
     * @return New QueueIconPicker instance
     */
    public static QueueIconPicker newInstance(@Nullable String currentIcon,
                                               @NonNull OnIconSelectedListener iconSelectedListener) {
        QueueIconPicker picker = new QueueIconPicker();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_ICON, currentIcon != null ? currentIcon : DEFAULT_ICON);
        picker.setArguments(args);
        picker.iconSelectedListener = iconSelectedListener;
        return picker;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            currentIcon = getArguments().getString(ARG_CURRENT_ICON, DEFAULT_ICON);
        }

        // Create RecyclerView for icon grid
        RecyclerView iconGrid = new RecyclerView(requireContext());
        iconGrid.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        iconGrid.setPadding(16, 16, 16, 16);

        // Set up adapter
        IconAdapter adapter = new IconAdapter(requireContext(), currentIcon);
        iconGrid.setAdapter(adapter);

        // Build dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Select Queue Icon");
        builder.setView(iconGrid);
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    /**
     * Adapter for icon grid display.
     */
    private class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {

        private final Context context;
        private final String selectedIcon;

        IconAdapter(@NonNull Context context, @NonNull String selectedIcon) {
            this.context = context;
            this.selectedIcon = selectedIcon;
        }

        @NonNull
        @Override
        public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.icon_picker_item, parent, false);
            return new IconViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
            IconEntry iconEntry = ICON_SET[position];
            holder.bind(iconEntry, iconEntry.name.equals(selectedIcon));
        }

        @Override
        public int getItemCount() {
            return ICON_SET.length;
        }

        /**
         * ViewHolder for icon items.
         */
        class IconViewHolder extends RecyclerView.ViewHolder {
            private final ImageView iconView;
            private final View selectionBorder;

            IconViewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.icon_view);
                selectionBorder = itemView.findViewById(R.id.selection_border);
            }

            void bind(@NonNull IconEntry iconEntry, boolean isSelected) {
                // Set icon drawable
                try {
                    Drawable drawable = ContextCompat.getDrawable(context, iconEntry.drawableRes);
                    iconView.setImageDrawable(drawable);
                } catch (Exception e) {
                    // If resource doesn't exist, use default
                    iconView.setImageResource(R.drawable.ic_playlist_play_24dp);
                }

                // Show/hide selection border
                selectionBorder.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                // Handle click
                itemView.setOnClickListener(v -> {
                    if (iconSelectedListener != null) {
                        iconSelectedListener.onIconSelected(iconEntry.name);
                    }
                    dismiss();
                });
            }
        }
    }

    /**
     * Gets the default icon name.
     *
     * @return Default icon name
     */
    @NonNull
    public static String getDefaultIcon() {
        return DEFAULT_ICON;
    }

    /**
     * Gets the icon set as an array of icon names.
     *
     * @return Array of icon names
     */
    @NonNull
    public static String[] getIconNames() {
        String[] names = new String[ICON_SET.length];
        for (int i = 0; i < ICON_SET.length; i++) {
            names[i] = ICON_SET[i].name;
        }
        return names;
    }

    /**
     * Resolves an icon name to its drawable resource ID.
     *
     * @param iconName Icon name (e.g., "ic_queue_music_24dp")
     * @return Drawable resource ID, or default icon if not found
     */
    @DrawableRes
    public static int resolveIconResource(@Nullable String iconName) {
        if (iconName == null || iconName.isEmpty()) {
            return R.drawable.ic_playlist_play_24dp;
        }
        for (IconEntry entry : ICON_SET) {
            if (entry.name.equals(iconName)) {
                return entry.drawableRes;
            }
        }
        return R.drawable.ic_playlist_play_24dp; // Default fallback
    }
}
