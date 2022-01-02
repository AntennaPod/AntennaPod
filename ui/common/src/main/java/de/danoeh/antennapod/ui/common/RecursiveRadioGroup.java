package de.danoeh.antennapod.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import java.util.ArrayList;

/**
 * An alternative to {@link android.widget.RadioGroup} that allows to nest children.
 * Basend on https://stackoverflow.com/a/14309274.
 */
public class RecursiveRadioGroup extends LinearLayout {
    private final ArrayList<RadioButton> radioButtons = new ArrayList<>();
    private RadioButton checkedButton = null;

    public RecursiveRadioGroup(Context context) {
        super(context);
    }

    public RecursiveRadioGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecursiveRadioGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        parseChild(child);
    }

    public void parseChild(final View child) {
        if (child instanceof RadioButton) {
            RadioButton button = (RadioButton) child;
            radioButtons.add(button);
            button.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    return;
                }
                checkedButton = (RadioButton) buttonView;

                for (RadioButton view : radioButtons) {
                    if (view != buttonView) {
                        view.setChecked(false);
                    }
                }
            });
        } else if (child instanceof ViewGroup) {
            parseChildren((ViewGroup) child);
        }
    }

    public void parseChildren(final ViewGroup child) {
        for (int i = 0; i < child.getChildCount(); i++) {
            parseChild(child.getChildAt(i));
        }
    }

    public RadioButton getCheckedButton() {
        return checkedButton;
    }
}
