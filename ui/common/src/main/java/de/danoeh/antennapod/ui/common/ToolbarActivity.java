package de.danoeh.antennapod.ui.common;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import de.danoeh.antennapod.ui.common.databinding.ToolbarActivityBinding;

/**
 * Activity showing a toolbar and ensuring that system insets are properly consumed.
 */
public class ToolbarActivity extends AppCompatActivity {
    private ToolbarActivityBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeSwitcher.getNoTitleTheme(this));
        super.onCreate(savedInstanceState);
        viewBinding = ToolbarActivityBinding.inflate(getLayoutInflater());
        setSupportActionBar(viewBinding.toolbar);
        super.setContentView(viewBinding.getRoot());
    }

    @Override
    public void setContentView(View view) {
        viewBinding.content.removeAllViews();
        viewBinding.content.addView(view);
    }

    @Override
    public void setContentView(int layoutResID) {
        setContentView(View.inflate(this, layoutResID, null));
    }
}
