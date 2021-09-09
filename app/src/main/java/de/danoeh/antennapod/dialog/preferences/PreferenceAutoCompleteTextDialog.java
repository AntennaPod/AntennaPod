package de.danoeh.antennapod.dialog.preferences;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.TagDialogBinding;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class PreferenceAutoCompleteTextDialog extends DialogFragment {
    public static final String TAG = "PrefAutoCompleteTextDlg";
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
            String text = viewBinding.newTagEditText.getText().toString().trim();
            if (onTextInputListener != null) {
                onTextInputListener.onTextInputListener(text);
            }
        });

        dialog.setNegativeButton(R.string.cancel_label, null);

        return dialog.create();
    }

    private void loadTags() {
        Observable.fromCallable(
                () -> {
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

    public interface AutoCompleteTextCallback {
        List<String> loadAutoCompleteText();
    }

    public interface OnTextInputListener {
        void onTextInputListener(String text);
    }
}
