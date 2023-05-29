package de.danoeh.antennapod.activity;

import static de.danoeh.antennapod.activity.MainActivity.EXTRA_FEED_ID;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.ThemeSwitcher;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.NavDrawerData;
import de.danoeh.antennapod.databinding.SubscriptionSelectionActivityBinding;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SelectSubscriptionActivity extends AppCompatActivity {

    private static final String TAG = "SelectSubscription";

    private Disposable disposable;
    private volatile List<Feed> listItems;

    private SubscriptionSelectionActivityBinding viewBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeSwitcher.getTranslucentTheme(this));
        super.onCreate(savedInstanceState);

        viewBinding = SubscriptionSelectionActivityBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        setSupportActionBar(viewBinding.toolbar);
        setTitle(R.string.shortcut_select_subscription);

        viewBinding.transparentBackground.setOnClickListener(v -> finish());
        viewBinding.card.setOnClickListener(null);

        loadSubscriptions();

        final Integer[] checkedPosition = new Integer[1];
        viewBinding.list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        viewBinding.list.setOnItemClickListener((listView, view1, position, rowId) ->
                checkedPosition[0] = position
        );
        viewBinding.shortcutBtn.setOnClickListener(view -> {
            if (checkedPosition[0] != null && Intent.ACTION_CREATE_SHORTCUT.equals(
                    getIntent().getAction())) {
                getBitmapFromUrl(listItems.get(checkedPosition[0]));
            }
        });

    }

    public List<Feed> getFeedItems(List<NavDrawerData.DrawerItem> items, List<Feed> result) {
        for (NavDrawerData.DrawerItem item : items) {
            if (item.type == NavDrawerData.DrawerItem.Type.TAG) {
                getFeedItems(((NavDrawerData.TagDrawerItem) item).children, result);
            } else {
                Feed feed = ((NavDrawerData.FeedDrawerItem) item).feed;
                if (!result.contains(feed)) {
                    result.add(feed);
                }
            }
        }
        return result;
    }

    private void addShortcut(Feed feed, Bitmap bitmap) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(EXTRA_FEED_ID, feed.getId());
        String id = "subscription-" + feed.getId();
        IconCompat icon;

        if (bitmap != null) {
            icon = IconCompat.createWithAdaptiveBitmap(bitmap);
        } else {
            icon = IconCompat.createWithResource(this, R.drawable.ic_subscriptions_shortcut);
        }

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, id)
                .setShortLabel(feed.getTitle())
                .setLongLabel(feed.getFeedTitle())
                .setIntent(intent)
                .setIcon(icon)
                .build();

        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, shortcut));
        finish();
    }

    private void getBitmapFromUrl(Feed feed) {
        int iconSize = (int) (128 * getResources().getDisplayMetrics().density);
        Glide.with(this)
                .asBitmap()
                .load(feed.getImageUrl())
                .apply(RequestOptions.overrideOf(iconSize, iconSize))
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Bitmap> target, boolean isFirstResource) {
                        addShortcut(feed, null);
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model,
                            Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        addShortcut(feed, resource);
                        return true;
                    }
                }).submit();
    }

    private void loadSubscriptions() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = Observable.fromCallable(
                () -> {
                    NavDrawerData data = DBReader.getNavDrawerData(UserPreferences.getSubscriptionsFilter());
                    return getFeedItems(data.items, new ArrayList<>());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            listItems = result;
                            ArrayList<String> titles = new ArrayList<>();
                            for (Feed feed: result) {
                                titles.add(feed.getTitle());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                    R.layout.simple_list_item_multiple_choice_on_start, titles);
                            viewBinding.list.setAdapter(adapter);
                        }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}