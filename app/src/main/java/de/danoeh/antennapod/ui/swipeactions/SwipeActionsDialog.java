package de.danoeh.antennapod.ui.swipeactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.gridlayout.widget.GridLayout;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.FeeditemlistItemBinding;
import de.danoeh.antennapod.databinding.SwipeactionsDialogBinding;
import de.danoeh.antennapod.databinding.SwipeactionsPickerBinding;
import de.danoeh.antennapod.databinding.SwipeactionsPickerItemBinding;
import de.danoeh.antennapod.databinding.SwipeactionsRowBinding;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import de.danoeh.antennapod.ui.screen.InboxFragment;
import de.danoeh.antennapod.ui.screen.PlaybackHistoryFragment;
import de.danoeh.antennapod.ui.screen.queue.QueueFragment;
import de.danoeh.antennapod.ui.common.ThemeUtils;

public class SwipeActionsDialog {
    private static final int LEFT = 1;
    private static final int RIGHT = 0;

    private final Context context;
    private final String tag;

    private SwipeAction rightAction;
    private SwipeAction leftAction;
    private List<SwipeAction> keys;

    public SwipeActionsDialog(Context context, String tag) {
        this.context = context;
        this.tag = tag;
    }

    public void show(Callback prefsChanged) {
        SwipeActions.Actions actions = SwipeActions.getPrefsWithDefaults(context, tag);
        leftAction = actions.left;
        rightAction = actions.right;

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

        keys = new ArrayList<>();
        if (tag.equals(QueueFragment.TAG)) {
            keys.add(new RemoveFromQueueSwipeAction());
        } else {
            keys.add(new AddToQueueSwipeAction());
        }
        if (!tag.equals(CompletedDownloadsFragment.TAG)) {
            keys.add(new StartDownloadSwipeAction());
        }
        if (!tag.equals(CompletedDownloadsFragment.TAG)
                && ! tag.equals(QueueFragment.TAG)
                && !tag.equals(PlaybackHistoryFragment.TAG)) {
            keys.add(new RemoveFromInboxSwipeAction());
        }
        if (!tag.equals(InboxFragment.TAG)) {
            keys.add(new DeleteSwipeAction());
        }
        keys.add(new MarkFavoriteSwipeAction());
        if (tag.equals(PlaybackHistoryFragment.TAG)) {
            keys.add(new RemoveFromHistorySwipeAction());
        }
        if (!tag.equals(InboxFragment.TAG)) {
            keys.add(new TogglePlaybackStateSwipeAction());
        }

        String forFragment = "";
        switch (tag) {
            case InboxFragment.TAG:
                forFragment = context.getString(R.string.inbox_label);
                break;
            case AllEpisodesFragment.TAG:
                forFragment = context.getString(R.string.episodes_label);
                break;
            case CompletedDownloadsFragment.TAG:
                forFragment = context.getString(R.string.downloads_label);
                break;
            case FeedItemlistFragment.TAG:
                forFragment = context.getString(R.string.individual_subscription);
                break;
            case QueueFragment.TAG:
                forFragment = context.getString(R.string.queue_label);
                break;
            case PlaybackHistoryFragment.TAG:
                forFragment = context.getString(R.string.playback_history_label);
                break;
            default: break;
        }

        builder.setTitle(context.getString(R.string.swipeactions_label) + " - " + forFragment);
        SwipeactionsDialogBinding viewBinding = SwipeactionsDialogBinding.inflate(LayoutInflater.from(context));
        builder.setView(viewBinding.getRoot());

        viewBinding.enableSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            viewBinding.actionLeftContainer.getRoot().setAlpha(b ? 1.0f : 0.4f);
            viewBinding.actionRightContainer.getRoot().setAlpha(b ? 1.0f : 0.4f);
        });

        viewBinding.enableSwitch.setChecked(SwipeActions.isSwipeActionEnabled(context, tag));

        setupSwipeDirectionView(viewBinding.actionLeftContainer, LEFT);
        setupSwipeDirectionView(viewBinding.actionRightContainer, RIGHT);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            savePrefs(tag, rightAction.getId(), leftAction.getId());
            saveActionsEnabledPrefs(viewBinding.enableSwitch.isChecked());
            prefsChanged.onCall();
        });

        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private void setupSwipeDirectionView(SwipeactionsRowBinding view, int direction) {
        SwipeAction action = direction == LEFT ? leftAction : rightAction;

        view.swipeDirectionLabel.setText(direction == LEFT ? R.string.swipe_left : R.string.swipe_right);
        view.swipeActionLabel.setText(action.getTitle(context));
        populateMockEpisode(view.mockEpisode);
        if (direction == RIGHT && view.previewContainer.getChildAt(0) != view.swipeIcon) {
            view.previewContainer.removeView(view.swipeIcon);
            view.previewContainer.addView(view.swipeIcon, 0);
        }

        view.swipeIcon.setImageResource(action.getActionIcon());
        view.swipeIcon.setColorFilter(ThemeUtils.getColorFromAttr(context, action.getActionColor()));

        view.changeButton.setOnClickListener(v -> showPicker(view, direction));
        view.previewContainer.setOnClickListener(v -> showPicker(view, direction));
    }

    private void showPicker(SwipeactionsRowBinding view, int direction) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(direction == LEFT ? R.string.swipe_left : R.string.swipe_right);

        SwipeactionsPickerBinding picker = SwipeactionsPickerBinding.inflate(LayoutInflater.from(context));
        builder.setView(picker.getRoot());
        builder.setNegativeButton(R.string.cancel_label, null);
        AlertDialog dialog = builder.show();

        for (int i = 0; i < keys.size(); i++) {
            final int actionIndex = i;
            SwipeAction action = keys.get(actionIndex);
            SwipeactionsPickerItemBinding item = SwipeactionsPickerItemBinding.inflate(LayoutInflater.from(context));
            item.swipeActionLabel.setText(action.getTitle(context));

            Drawable icon = DrawableCompat.wrap(AppCompatResources.getDrawable(context, action.getActionIcon()));
            icon.mutate();
            icon.setTintMode(PorterDuff.Mode.SRC_ATOP);
            if ((direction == LEFT && leftAction == action) || (direction == RIGHT && rightAction == action)) {
                icon.setTint(ThemeUtils.getColorFromAttr(context, action.getActionColor()));
                item.swipeActionLabel.setTextColor(ThemeUtils.getColorFromAttr(context, action.getActionColor()));
            } else {
                icon.setTint(ThemeUtils.getColorFromAttr(context, R.attr.action_icon_color));
            }
            item.swipeIcon.setImageDrawable(icon);

            item.getRoot().setOnClickListener(v -> {
                if (direction == LEFT) {
                    leftAction = keys.get(actionIndex);
                } else {
                    rightAction = keys.get(actionIndex);
                }
                setupSwipeDirectionView(view, direction);
                dialog.dismiss();
            });
            GridLayout.LayoutParams param = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.BASELINE),
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f));
            param.width  = 0;
            picker.pickerGridLayout.addView(item.getRoot(), param);
        }
        picker.pickerGridLayout.setColumnCount(2);
        picker.pickerGridLayout.setRowCount((keys.size() + 1) / 2);
    }

    private void populateMockEpisode(FeeditemlistItemBinding view) {
        view.container.setAlpha(0.3f);
        view.secondaryActionButton.secondaryActionButton.setVisibility(View.GONE);
        view.dragHandle.setVisibility(View.GONE);
        view.statusInbox.setVisibility(View.GONE);
        view.txtvTitle.setText("███████");
        view.txtvPosition.setText("█████");
    }

    private void savePrefs(String tag, String right, String left) {
        SharedPreferences prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(SwipeActions.KEY_PREFIX_SWIPEACTIONS + tag, right + "," + left).apply();
    }

    private void saveActionsEnabledPrefs(Boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(SwipeActions.KEY_PREFIX_NO_ACTION + tag, enabled).apply();
    }

    public interface Callback {
        void onCall();
    }
}
