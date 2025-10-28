package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.danoeh.antennapod.model.feed.Queue;

/**
 * Custom view displaying a gradient header with active queue color.
 *
 * <p>This view provides visual feedback for the currently active queue by displaying
 * a gradient from the queue's color to transparent. It includes optional display of
 * the queue name and icon, and can be hidden when only the default queue exists.
 *
 * <p>Features:
 * - Gradient shader from queue color (top) to transparent (bottom)
 * - Optional queue name and icon display
 * - Automatic visibility management based on queue count
 * - Handles configuration changes (rotation, theme changes)
 *
 * <p>Usage in XML:
 * <pre>
 * &lt;de.danoeh.antennapod.ui.common.QueueGradientHeader
 *     android:id="@+id/queue_gradient_header"
 *     android:layout_width="match_parent"
 *     android:layout_height="56dp" /&gt;
 * </pre>
 *
 * <p>Usage in code:
 * <pre>
 * QueueGradientHeader header = findViewById(R.id.queue_gradient_header);
 * header.setQueue(activeQueue);
 * header.setVisibleIfMultipleQueues(queueCount);
 * </pre>
 */
public class QueueGradientHeader extends FrameLayout {

    private static final int DEFAULT_COLOR = Color.parseColor("#2196F3"); // Material Blue
    private static final int DEFAULT_HEIGHT_DP = 56;

    private View gradientView;
    private LinearLayout contentLayout;
    private ImageView iconView;
    private TextView nameView;

    private Queue currentQueue;
    private int currentColor = DEFAULT_COLOR;
    private boolean showNameAndIcon = false;

    /**
     * Constructor for programmatic creation.
     *
     * @param context Activity or application context
     */
    public QueueGradientHeader(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor for XML inflation.
     *
     * @param context Activity or application context
     * @param attrs   Attribute set from XML
     */
    public QueueGradientHeader(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor for XML inflation with style.
     *
     * @param context      Activity or application context
     * @param attrs        Attribute set from XML
     * @param defStyleAttr Default style attribute
     */
    public QueueGradientHeader(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Initializes the view hierarchy.
     *
     * @param context Activity or application context
     */
    private void init(@NonNull Context context) {
        // Create gradient background view
        gradientView = new View(context);
        LayoutParams gradientParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        gradientView.setLayoutParams(gradientParams);
        addView(gradientView);

        // Create content layout for optional icon and name
        contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        int paddingPx = dpToPx(12);
        contentLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        LayoutParams contentParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        contentLayout.setLayoutParams(contentParams);
        contentLayout.setVisibility(GONE); // Hidden by default
        addView(contentLayout);

        // Create icon view
        iconView = new ImageView(context);
        int iconSizePx = dpToPx(24);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSizePx, iconSizePx);
        iconParams.setMargins(0, 0, dpToPx(8), 0);
        iconView.setLayoutParams(iconParams);
        contentLayout.addView(iconView);

        // Create name view
        nameView = new TextView(context);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(16);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameView.setLayoutParams(nameParams);
        contentLayout.addView(nameView);

        // Set default gradient
        updateGradient(DEFAULT_COLOR);
    }

    /**
     * Sets the queue to display.
     *
     * <p>Updates the gradient color and optionally displays the queue name
     * if {@link #setShowNameAndIcon(boolean)} is enabled.
     *
     * @param queue Queue to display (null uses default color)
     */
    public void setQueue(@Nullable Queue queue) {
        this.currentQueue = queue;
        if (queue != null) {
            // Use default color for all queues (color customization removed for MVP)
            setQueueColor(DEFAULT_COLOR);
            if (showNameAndIcon) {
                nameView.setText(queue.getName());
                contentLayout.setVisibility(VISIBLE);
            }
        } else {
            setQueueColor(DEFAULT_COLOR);
            if (showNameAndIcon) {
                nameView.setText("");
                contentLayout.setVisibility(GONE);
            }
        }
    }

    /**
     * Sets the queue color and updates the gradient.
     *
     * <p>Handles "no color" (0 or transparent) gracefully by using the default color.
     *
     * @param color ARGB color value
     */
    public void setQueueColor(@ColorInt int color) {
        // Handle transparent/no color case
        if (color == 0 || Color.alpha(color) == 0) {
            color = DEFAULT_COLOR;
        }
        this.currentColor = color;
        updateGradient(color);
    }

    /**
     * Shows or hides the header based on queue count.
     *
     * <p>The header is hidden when only the default queue exists (queueCount == 1)
     * to avoid visual clutter. It's shown when multiple queues exist to provide
     * visual context for the active queue.
     *
     * @param queueCount Total number of queues in the system
     */
    public void setVisibleIfMultipleQueues(int queueCount) {
        setVisibility(queueCount > 1 ? VISIBLE : GONE);
    }

    /**
     * Enables or disables display of queue name and icon.
     *
     * <p>By default, only the gradient is shown. Enable this to also show
     * the queue name and icon in the header.
     *
     * @param show True to show name and icon, false to show only gradient
     */
    public void setShowNameAndIcon(boolean show) {
        this.showNameAndIcon = show;
        contentLayout.setVisibility(show && currentQueue != null ? VISIBLE : GONE);
        if (show && currentQueue != null) {
            nameView.setText(currentQueue.getName());
        }
    }

    /**
     * Gets whether name and icon display is enabled.
     *
     * @return True if name and icon are shown, false if only gradient is shown
     */
    public boolean isShowNameAndIcon() {
        return showNameAndIcon;
    }

    /**
     * Gets the currently displayed queue.
     *
     * @return Current queue, or null if no queue is set
     */
    @Nullable
    public Queue getCurrentQueue() {
        return currentQueue;
    }

    /**
     * Gets the current gradient color.
     *
     * @return Current ARGB color value
     */
    @ColorInt
    public int getCurrentColor() {
        return currentColor;
    }

    /**
     * Updates the gradient drawable with the given color.
     *
     * <p>Creates a gradient from the queue color (top) to transparent (bottom)
     * for a subtle visual effect.
     *
     * @param color ARGB color for gradient start
     */
    private void updateGradient(@ColorInt int color) {
        GradientDrawable gradient = createGradientDrawable(color);
        gradientView.setBackground(gradient);
    }

    /**
     * Creates a gradient drawable from queue color to transparent.
     *
     * <p>Gradient direction: top to bottom, starting with the queue color
     * and fading to fully transparent at the bottom.
     *
     * @param color ARGB color for gradient start
     * @return GradientDrawable configured with the color gradient
     */
    @NonNull
    private GradientDrawable createGradientDrawable(@ColorInt int color) {
        // Create gradient from color (top) to transparent (bottom)
        int[] colors = new int[]{
                color,                          // Start: queue color (opaque)
                Color.TRANSPARENT               // End: transparent
        };

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                colors
        );

        // Optional: set shape to rectangle (default, but explicit is clearer)
        gradient.setShape(GradientDrawable.RECTANGLE);

        return gradient;
    }

    /**
     * Converts density-independent pixels to pixels.
     *
     * @param dp Value in dp
     * @return Value in pixels
     */
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Gets the default color used when no queue color is set.
     *
     * @return Default ARGB color value (Material Blue)
     */
    @ColorInt
    public static int getDefaultColor() {
        return DEFAULT_COLOR;
    }
}
