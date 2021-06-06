package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.SwipeActions;

public class SwipeActionsDialog {

    private final Context context;
    private final String tag;

    public SwipeActionsDialog(Context context, String tag) {
        this.context = context;
        this.tag = tag;
    }

    public void show(Callback prefsChanged, Callback dismissed) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        String forFragment = "";
        switch (tag) {
            /*case InboxFragment.TAG:
                forFragment = context.getString(R.string.inbox_label);
                break;*/
            case EpisodesFragment.TAG:
                forFragment = context.getString(R.string.episodes_label);
                break;
            case FeedItemlistFragment.TAG:
                forFragment = context.getString(R.string.feeds_label);
                break;
            default: break;
        }

        builder.setTitle(context.getString(R.string.swipeactions_label) + " - " + forFragment);

        //same order as in R.array.swipe_actions
        final List<String> prefKeys = Arrays.asList(SwipeActions.ADD_TO_QUEUE,
                SwipeActions.MARK_UNPLAYED, SwipeActions.START_DOWNLOAD,
                SwipeActions.MARK_FAV, SwipeActions.MARK_PLAYED);

        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.swipeactions_dialog, null, false);

        final ImageView rightIcon = layout.findViewById(R.id.swipeactionIconRight);
        final ImageView leftIcon = layout.findViewById(R.id.swipeactionIconLeft);
        final View rightColor = layout.findViewById(R.id.swipeColorViewRight);
        final View leftColor = layout.findViewById(R.id.swipeColorViewLeft);

        Spinner spinnerRightAction = layout.findViewById(R.id.spinnerRightAction);
        Spinner spinnerLeftAction = layout.findViewById(R.id.spinnerLeftAction);

        rightColor.setOnClickListener(view -> spinnerRightAction.performClick());
        leftColor.setOnClickListener(view -> spinnerLeftAction.performClick());

        spinnerRightAction.setAdapter(adapter());
        spinnerLeftAction.setAdapter(adapter());

        spinnerRightAction.setOnItemSelectedListener(
                listener((a, v, i, l) -> {
                    String action = prefKeys.get(i);
                    rightIcon.setImageResource(SwipeActions.actionIconFor(action));
                    rightColor.setBackgroundResource(SwipeActions.actionColorFor(action));
                })
        );
        spinnerLeftAction.setOnItemSelectedListener(
                listener((a, v, i, l) -> {
                    String action = prefKeys.get(i);
                    leftIcon.setImageResource(SwipeActions.actionIconFor(action));
                    leftColor.setBackgroundResource(SwipeActions.actionColorFor(action));
                })
        );

        //load prefs and suggest defaults if swiped the first time
        String[] rightleft = SwipeActions.getPrefsWithDefaults(context, tag);
        int right = prefKeys.indexOf(rightleft[0]);
        int left = prefKeys.indexOf(rightleft[1]);

        spinnerRightAction.setSelection(right);
        spinnerLeftAction.setSelection(left);

        builder.setView(layout);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            String rightAction = prefKeys.get(spinnerRightAction.getSelectedItemPosition());
            String leftAction = prefKeys.get(spinnerLeftAction.getSelectedItemPosition());
            savePrefs(tag, rightAction, leftAction);
            prefsChanged.onCall();
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.setOnDismissListener(dialogInterface -> dismissed.onCall());
        builder.create().show();
    }

    private void savePrefs(String tag, String right, String left) {
        SharedPreferences prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(SwipeActions.PREF_SWIPEACTIONS + tag, right + "," + left).apply();
    }

    private AdapterView.OnItemSelectedListener listener(AdapterView.OnItemClickListener listener) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                listener.onItemClick(adapterView, view, i, l);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
    }

    private ArrayAdapter<String> adapter() {
        return new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                context.getResources().getStringArray(R.array.swipe_actions));
    }

    public interface Callback {
        void onCall();
    }
}
