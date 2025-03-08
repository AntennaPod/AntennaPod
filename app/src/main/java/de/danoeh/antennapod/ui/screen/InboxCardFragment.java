package de.danoeh.antennapod.ui.screen;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import de.danoeh.antennapod.databinding.InboxCardBinding;
import de.danoeh.antennapod.databinding.InboxCardFragmentBinding;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.screen.episode.ItemFragment;
import de.danoeh.antennapod.ui.swipeactions.SwipeActions;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Tinder-like card view
 */
public class InboxCardFragment extends Fragment {
    private InboxCardFragmentBinding viewBinding;
    private FeedItem firstInboxItem;
    private Disposable disposable;
    private boolean swipingEnabled = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final SwipeActions.Actions actions = SwipeActions.getPrefsWithDefaults(getContext(), InboxFragment.TAG);

        viewBinding = InboxCardFragmentBinding.inflate(inflater);
        viewBinding.cardPager.setOffscreenPageLimit(1);
        viewBinding.cardPager.setCurrentItem(1, false);
        viewBinding.cardPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    swipingEnabled = true;
                    return;
                }
                if (!swipingEnabled) {
                    return;
                }
                swipingEnabled = false;
                viewBinding.fakeCard.card.animate().setStartDelay(10)
                        .scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                viewBinding.cardPager.animate().setStartDelay(10).alpha(0.01f).setDuration(100).withEndAction(() -> {
                    if (position == 2) {
                        actions.left.performAction(firstInboxItem,
                                InboxCardFragment.this, FeedItemFilter.unfiltered());
                    } else if (position == 0) {
                        actions.right.performAction(firstInboxItem,
                                InboxCardFragment.this, FeedItemFilter.unfiltered());
                    }
                }).start();
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (position == 0) {
                    viewBinding.leftButton.setAlpha(positionOffset);
                    float scale = 1.0f + (1.0f - positionOffset) / 3;
                    viewBinding.rightButton.setScaleX(scale);
                    viewBinding.rightButton.setScaleY(scale);
                } else if (position == 1 && positionOffset != 0.0f) {
                    float scale = 1.0f + positionOffset / 3;
                    viewBinding.leftButton.setScaleX(scale);
                    viewBinding.leftButton.setScaleY(scale);
                    viewBinding.rightButton.setAlpha(1.0f - positionOffset);
                } else {
                    viewBinding.leftButton.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(200).start();
                    viewBinding.rightButton.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(200).start();
                }
            }
        });

        viewBinding.leftButton.setImageResource(actions.left.getActionIcon());
        viewBinding.leftButton.getBackground().mutate()
                .setColorFilter(new PorterDuffColorFilter(actions.left.getActionColor(), PorterDuff.Mode.SRC_ATOP));
        viewBinding.leftButton.setOnClickListener(v -> viewBinding.cardPager.setCurrentItem(2, true));
        viewBinding.rightButton.setImageResource(actions.right.getActionIcon());
        viewBinding.rightButton.getBackground().mutate()
                .setColorFilter(new PorterDuffColorFilter(actions.right.getActionColor(), PorterDuff.Mode.SRC_ATOP));
        viewBinding.rightButton.setOnClickListener(v -> viewBinding.cardPager.setCurrentItem(0, true));

        return viewBinding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(() -> DBReader.getEpisodes(0, 1,
                        new FeedItemFilter(FeedItemFilter.NEW),  UserPreferences.getInboxSortedOrder()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(items -> {
                    if (items.isEmpty()) {
                        firstInboxItem = null;
                        viewBinding.cardPager.setVisibility(View.GONE);
                        viewBinding.fakeCard.card.setVisibility(View.GONE);
                        viewBinding.leftButton.setVisibility(View.GONE);
                        viewBinding.rightButton.setVisibility(View.GONE);
                        viewBinding.emptyLabel.setVisibility(View.VISIBLE);
                    } else {
                        firstInboxItem = items.get(0);
                    }
                    viewBinding.cardPager.setAdapter(new ItemPagerAdapter(this));
                    viewBinding.cardPager.setCurrentItem(1, false);
                    viewBinding.cardPager.animate().setStartDelay(200).alpha(1).setDuration(200).withEndAction(() -> {
                        viewBinding.fakeCard.card.animate().scaleX(0.9f).scaleY(0.9f).setDuration(0).start();
                    }).start();
                }, Throwable::printStackTrace);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UnreadItemsUpdateEvent event) {
        loadItems();
    }

    private class ItemPagerAdapter extends FragmentStateAdapter {

        ItemPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 1 && firstInboxItem != null) {
                return CardItemFragment.newInstance(firstInboxItem.getId());
            } else {
                return new EmptyFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    public static class EmptyFragment extends Fragment {

    }

    public static class CardItemFragment extends ItemFragment {
        public static CardItemFragment newInstance(long feeditem) {
            CardItemFragment fragment = new CardItemFragment();
            Bundle args = new Bundle();
            args.putLong(ARG_FEEDITEM, feeditem);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater,
                     @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            InboxCardBinding viewBinding = InboxCardBinding.inflate(inflater);
            View child = super.onCreateView(inflater, viewBinding.cardContentFrame, savedInstanceState);
            viewBinding.cardContentFrame.addView(child);
            return viewBinding.getRoot();
        }
    }
}
