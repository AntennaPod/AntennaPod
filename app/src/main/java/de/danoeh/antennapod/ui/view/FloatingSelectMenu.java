package de.danoeh.antennapod.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
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
        addView(viewBinding.getRoot());
        setVisibility(View.GONE);
    }

    public void inflate(@MenuRes int menuRes) {
        PopupMenu popupMenu = new PopupMenu(getContext(), new View(getContext()));
        popupMenu.inflate(menuRes);
        menu = popupMenu.getMenu();
        show();
    }

    public void show() {
        viewBinding.selectContainer.removeAllViews();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            FloatingSelectMenuItemBinding itemBinding = FloatingSelectMenuItemBinding.bind(
                    View.inflate(getContext(), R.layout.floating_select_menu_item, null));
            itemBinding.titleLabel.setText(item.getTitle());
            itemBinding.icon.setImageDrawable(item.getIcon());
            itemBinding.getRoot().setOnClickListener(view -> menuItemClickListener.onMenuItemClick(item));
            viewBinding.selectContainer.addView(itemBinding.getRoot());
        }
    }

    public void setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener listener) {
        this.menuItemClickListener = listener;
    }

    private void removeItemById(@IdRes int itemId) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == itemId) {
                menu.removeItem(i);
                return;
            }
        }
    }

    public void removeItemsById(@IdRes int... itemIds) {
        for (int itemId : itemIds) {
            removeItemById(itemId);
        }
        show();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        viewBinding.scrollView.scrollTo(0, 0);
    }
}
