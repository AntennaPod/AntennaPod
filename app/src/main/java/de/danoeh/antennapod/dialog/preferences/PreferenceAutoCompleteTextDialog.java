package de.danoeh.antennapod.dialog.preferences;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.databinding.TagDialogBinding;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class PreferenceAutoCompleteTextDialog extends DialogFragment {
    public static final String TAG = "TagSettingsDialog";
    private static final String ARG_FEED_PREFERENCES = "feed_preferences";
    private List<String> displayedTags;
    private @NonNull TagDialogBinding viewBinding;
    private String title;
    private AutoCompleteTextCallback autoCompleteTextCallback;
    private OnTextInputListener onTextInputListener;
    public PreferenceAutoCompleteTextDialog(String title, AutoCompleteTextCallback callback,
                                            OnTextInputListener onTextInputListener) {
        this.title = title;
        this.autoCompleteTextCallback = callback;
        this.onTextInputListener = onTextInputListener;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
//        FeedPreferences preferences = (FeedPreferences) getArguments().getSerializable(ARG_FEED_PREFERENCES);
//        displayedTags = new ArrayList<>(preferences.getTags());
//        displayedTags.remove(FeedPreferences.TAG_ROOT);

        viewBinding = TagDialogBinding.inflate(getLayoutInflater());

        loadTags();
        viewBinding.newTagEditText.setThreshold(1);
        viewBinding.newTagEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                viewBinding.newTagEditText.showDropDown();
                viewBinding.newTagEditText.requestFocus();
                return false;
            }
        });

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setView(viewBinding.getRoot());
        dialog.setTitle(title);
        dialog.setPositiveButton(android.R.string.ok, (d, input) -> {
//            addTag();
            String text = viewBinding.newTagEditText.getText().toString().trim();
            if (onTextInputListener != null) {
                onTextInputListener.onTextInputListener(text);
            }
//            preferences.getTags().clear();
//            preferences.getTags().addAll(displayedTags);
//            DBWriter.setFeedPreferences(preferences);
        });
        dialog.setNegativeButton(R.string.cancel_label, null);
        return dialog.create();
    }

    public interface AutoCompleteTextCallback {
        List<String> loadAutoCompleteText();
    }

    public interface OnTextInputListener {
        void onTextInputListener(String text);
    }
    private void loadTags() {
        Observable.fromCallable(
                () -> {
//                    NavDrawerData data = DBReader.getNavDrawerData();
//                    List<NavDrawerData.DrawerItem> items = data.items;
//                    List<String> folders = new ArrayList<String>();
//                    for (NavDrawerData.DrawerItem item : items) {
//                        if (item.type == NavDrawerData.DrawerItem.Type.FOLDER) {
//                            folders.add(item.getTitle());
//                        }
//                    }
//                    return folders;
                    if (autoCompleteTextCallback != null) {
                        return  autoCompleteTextCallback.loadAutoCompleteText();
                    }

                    return null;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null) {
                                ArrayAdapter<String> acAdapter = new ArrayAdapter<String>(getContext(),
                                        R.layout.single_tag_text_view, result);
                                viewBinding.newTagEditText.setAdapter(acAdapter);
                            }
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                        });
    }

    private void addTag(String name) {
        if (TextUtils.isEmpty(name) || displayedTags.contains(name)) {
            return;
        }
        displayedTags.add(name);
        viewBinding.newTagEditText.setText("");
    }

}
