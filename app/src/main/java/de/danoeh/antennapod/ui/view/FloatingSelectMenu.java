package de.danoeh.antennapod.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import com.google.android.material.elevation.SurfaceColors;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.FloatingSelectMenuBinding;
import de.danoeh.antennapod.databinding.FloatingSelectMenuItemBinding;

public class FloatingSelectMenu extends FrameLayout {
    private FloatingSelectMenuBinding viewBinding;
    private Menu menu;
    private MenuItem.OnMenuItemClickListener menuItemClickListener;

    public FloatingSelectMenu(@NonNull Context context) {
        super(context);
        setup();
    }

    public FloatingSelectMenu(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public FloatingSelectMenu(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        viewBinding = FloatingSelectMenuBinding.bind(
                View.inflate(getContext(), R.layout.floating_select_menu, null));
        viewBinding.card.setCardBackgroundColor(
                SurfaceColors.getColorForElevation(getContext(), 8 * getResources().getDisplayMetrics().density));
        addView(viewBinding.getRoot());
        setVisibility(View.GONE);
    }

    public void inflate(@MenuRes int menuRes) {
        PopupMenu popupMenu = new PopupMenu(getContext(), new View(getContext()));
        popupMenu.inflate(menuRes);
        menu = popupMenu.getMenu();
        updateItemVisibility();
    }

    public void updateItemVisibility() {
        viewBinding.selectContainer.removeAllViews();
        if (menu == null) {
            return;
        }
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (!item.isVisible()) {
                continue;
            }
            FloatingSelectMenuItemBinding itemBinding = FloatingSelectMenuItemBinding.bind(
                    View.inflate(getContext(), R.layout.floating_select_menu_item, null));
            itemBinding.titleLabel.setText(item.getTitle());
            itemBinding.icon.setImageDrawable(item.getIcon());
            itemBinding.getRoot().setOnClickListener(view -> menuItemClickListener.onMenuItemClick(item));
            viewBinding.selectContainer.addView(itemBinding.getRoot());
        }
    }

    public Menu getMenu() {
        return menu;
    }

    public void setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener listener) {
        this.menuItemClickListener = listener;
    }

    @Override
    public void setVisibility(int visibility) {
        if (getVisibility() != View.VISIBLE && visibility == View.VISIBLE) {
            announceForAccessibility(getContext().getString(R.string.multi_select_started_talkback));
        }
        super.setVisibility(visibility);
        viewBinding.scrollView.scrollTo(0, 0);
        updateItemVisibility();
    }
}
