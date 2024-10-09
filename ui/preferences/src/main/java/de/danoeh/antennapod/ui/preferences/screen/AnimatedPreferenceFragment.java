package de.danoeh.antennapod.ui.preferences.screen;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.transition.MaterialSharedAxis;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.preferences.R;

public abstract class AnimatedPreferenceFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(ThemeUtils.getColorFromAttr(getContext(), R.attr.colorSurface));
    }
}
