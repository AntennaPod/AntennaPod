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

import com.annimon.stream.Stream;

import java.util.Arrays;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.swipeactions.SwipeAction;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;

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
            case QueueFragment.TAG:
                forFragment = context.getString(R.string.queue_label);
                break;
            default: break;
        }

        builder.setTitle(context.getString(R.string.swipeactions_label) + " - " + forFragment);

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

        spinnerRightAction.setOnItemSelectedListener(listener((a, v, i, l) -> {
            rightIcon.setImageResource(SwipeActions.swipeActions.get(i).actionIcon());
            rightColor.setBackgroundResource(SwipeActions.swipeActions.get(i).actionColor());
        }));
        spinnerLeftAction.setOnItemSelectedListener(listener((a, v, i, l) -> {
            leftIcon.setImageResource(SwipeActions.swipeActions.get(i).actionIcon());
            leftColor.setBackgroundResource(SwipeActions.swipeActions.get(i).actionColor());
        }));

        //load prefs and suggest defaults if swiped the first time
        int[] rightleft = SwipeActions.getPrefsWithDefaults(context, tag);
        int right = rightleft[0];
        int left = rightleft[1];

        spinnerRightAction.setSelection(right);
        spinnerLeftAction.setSelection(left);

        builder.setView(layout);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            int rightAction = spinnerRightAction.getSelectedItemPosition();
            int leftAction = spinnerLeftAction.getSelectedItemPosition();
            savePrefs(tag, rightAction, leftAction);
            prefsChanged.onCall();
        });
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.setOnDismissListener(dialogInterface -> dismissed.onCall());
        builder.create().show();
    }

    private void savePrefs(String tag, int right, int left) {
        SharedPreferences prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(SwipeActions.PREF_SWIPEACTION_RIGHT + tag, right).apply();
        prefs.edit().putInt(SwipeActions.PREF_SWIPEACTION_LEFT + tag, left).apply();
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
        final List<String> titles = Stream.of(SwipeActions.swipeActions)
                .map(swa -> swa.title(context))
                .toList();

        return new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item,
                titles);
    }

    public interface Callback {
        void onCall();
    }
}
