package de.danoeh.antennapod.ui.screen.feed;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.databinding.FeedinfoBinding;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.ui.TransitionEffect;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.common.ClipboardUtils;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.share.ShareUtils;
import de.danoeh.antennapod.ui.cleaner.HtmlToPlainText;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedFunding;
import de.danoeh.antennapod.ui.glide.FastBlurTransformation;
import de.danoeh.antennapod.ui.statistics.StatisticsFragment;
import de.danoeh.antennapod.ui.statistics.feed.FeedStatisticsDialogFragment;
import de.danoeh.antennapod.ui.statistics.feed.FeedStatisticsFragment;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeOnSubscribe;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Displays information about a feed.
 */
public class FeedInfoFragment extends Fragment implements MaterialToolbar.OnMenuItemClickListener {

    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String TAG = "FeedInfoActivity";

    private Feed feed;
    private Disposable disposable;
    private FeedinfoBinding viewBinding;

    public static FeedInfoFragment newInstance(Feed feed) {
        FeedInfoFragment fragment = new FeedInfoFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_FEED_ID, feed.getId());
        fragment.setArguments(arguments);
        return fragment;
    }

    private final View.OnClickListener copyUrlToClipboard = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (feed != null && feed.getDownloadUrl() != null) {
                ClipboardUtils.copyText(v, R.string.url_label, feed.getDownloadUrl());
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewBinding = FeedinfoBinding.inflate(inflater);
        viewBinding.toolbar.setTitle("");
        viewBinding.toolbar.inflateMenu(R.menu.feedinfo);
        viewBinding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        viewBinding.toolbar.setOnMenuItemClickListener(this);
        refreshToolbarState();

        ToolbarIconTintManager iconTintManager =
                new ToolbarIconTintManager(viewBinding.toolbar, viewBinding.collapsingToolbar);
        viewBinding.appBar.addOnOffsetChangedListener(iconTintManager);

        viewBinding.header.butShowInfo.setVisibility(View.INVISIBLE);
        viewBinding.header.butShowSettings.setVisibility(View.INVISIBLE);
        viewBinding.header.butFilter.setVisibility(View.INVISIBLE);
        // https://github.com/bumptech/glide/issues/529
        viewBinding.imgvBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));
        viewBinding.urlLabel.setOnClickListener(copyUrlToClipboard);

        long feedId = getArguments().getLong(EXTRA_FEED_ID);
        getParentFragmentManager().beginTransaction().replace(R.id.statisticsFragmentContainer,
                        FeedStatisticsFragment.newInstance(feedId, false), "feed_statistics_fragment")
                .commitAllowingStateLoss();
        viewBinding.statisticsFragmentContainer.setOnClickListener(v ->
                FeedStatisticsDialogFragment.newInstance(feedId, feed.getTitle())
                        .show(getChildFragmentManager().beginTransaction(), "FeedStatistics"));

        viewBinding.statisticsButton.setOnClickListener(view -> {
            StatisticsFragment fragment = new StatisticsFragment();
            ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
        });
        viewBinding.header.txtvTitle.setOnLongClickListener(v -> {
            ClipboardUtils.copyText(viewBinding.header.txtvTitle);
            return true;
        });
        viewBinding.header.txtvAuthor.setOnLongClickListener(v -> {
            ClipboardUtils.copyText(viewBinding.header.txtvAuthor);
            return true;
        });
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long feedId = getArguments().getLong(EXTRA_FEED_ID);
        disposable = Maybe.create((MaybeOnSubscribe<Feed>) emitter -> {
            Feed feed = DBReader.getFeed(feedId, false, 0, 0);
            if (feed != null) {
                emitter.onSuccess(feed);
            } else {
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    feed = result;
                    showFeed();
                }, error -> Log.d(TAG, Log.getStackTraceString(error)), () -> { });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (viewBinding == null) {
            return;
        }
        int horizontalSpacing = (int) getResources().getDimension(R.dimen.additional_horizontal_spacing);
        viewBinding.header.getRoot().setPadding(horizontalSpacing, viewBinding.header.getRoot().getPaddingTop(),
                horizontalSpacing, viewBinding.header.getRoot().getPaddingBottom());
        viewBinding.infoContainer.setPadding(horizontalSpacing, viewBinding.infoContainer.getPaddingTop(),
                horizontalSpacing, viewBinding.infoContainer.getPaddingBottom());
    }

    private void showFeed() {
        Log.d(TAG, "Language is " + feed.getLanguage());
        Log.d(TAG, "Author is " + feed.getAuthor());
        Log.d(TAG, "URL is " + feed.getDownloadUrl());
        Glide.with(this)
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .fitCenter()
                        .dontAnimate())
                .into(viewBinding.header.imgvCover);
        Glide.with(this)
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                        .placeholder(R.color.image_readability_tint)
                        .error(R.color.image_readability_tint)
                        .transform(new FastBlurTransformation())
                        .dontAnimate())
                .into(viewBinding.imgvBackground);

        viewBinding.header.txtvTitle.setText(feed.getTitle());
        viewBinding.header.txtvTitle.setMaxLines(6);

        String description = HtmlToPlainText.getPlainText(feed.getDescription());

        viewBinding.descriptionLabel.setText(description);

        if (!TextUtils.isEmpty(feed.getAuthor())) {
            viewBinding.header.txtvAuthor.setText(feed.getAuthor());
        }

        viewBinding.urlLabel.setText(feed.getDownloadUrl());
        viewBinding.urlLabel.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_paperclip, 0);

        if (feed.getPaymentLinks() == null || feed.getPaymentLinks().size() == 0) {
            viewBinding.supportHeadingLabel.setVisibility(View.GONE);
            viewBinding.supportUrl.setVisibility(View.GONE);
        } else {
            ArrayList<FeedFunding> fundingList = feed.getPaymentLinks();

            // Filter for duplicates, but keep items in the order that they have in the feed.
            Iterator<FeedFunding> i = fundingList.iterator();
            while (i.hasNext()) {
                FeedFunding funding = i.next();
                for (FeedFunding other : fundingList) {
                    if (TextUtils.equals(other.url, funding.url)) {
                        if (other.content != null && funding.content != null
                                && other.content.length() > funding.content.length()) {
                            i.remove();
                            break;
                        }
                    }
                }
            }

            StringBuilder str = new StringBuilder();
            for (FeedFunding funding : fundingList) {
                str.append(funding.content.isEmpty()
                        ? getContext().getResources().getString(R.string.support_podcast)
                        : funding.content).append(" ").append(funding.url);
                str.append("\n");
            }
            str = new StringBuilder(StringUtils.trim(str.toString()));
            viewBinding.supportUrl.setText(str.toString());
        }

        if (feed.getState() == Feed.STATE_NOT_SUBSCRIBED) {
            viewBinding.statisticsHeading.setVisibility(View.GONE);
            viewBinding.statisticsFragmentContainer.setVisibility(View.GONE);
            viewBinding.supportHeadingLabel.setVisibility(View.GONE);
            viewBinding.supportUrl.setVisibility(View.GONE);
            viewBinding.header.butSubscribe.setVisibility(View.VISIBLE);
            viewBinding.header.butSubscribe.setOnClickListener(view -> {
                DBWriter.setFeedState(getContext(), feed, Feed.STATE_SUBSCRIBED);
                MainActivityStarter mainActivityStarter = new MainActivityStarter(getContext());
                mainActivityStarter.withOpenFeed(feed.getId());
                mainActivityStarter.withClearBackStack();
                getActivity().finish();
                startActivity(mainActivityStarter.getIntent());
            });
        } else {
            long feedId = getArguments().getLong(EXTRA_FEED_ID);
            getParentFragmentManager().beginTransaction().replace(R.id.statisticsFragmentContainer,
                            FeedStatisticsFragment.newInstance(feedId, false), "feed_statistics_fragment")
                    .commitAllowingStateLoss();

            viewBinding.statisticsButton.setOnClickListener(view -> {
                StatisticsFragment fragment = new StatisticsFragment();
                ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
            });
        }

        refreshToolbarState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
        }
        viewBinding = null;
    }

    private void refreshToolbarState() {
        boolean isSubscribed = feed != null && feed.getState() == Feed.STATE_SUBSCRIBED;
        viewBinding.toolbar.getMenu().findItem(R.id.share_item).setVisible(isSubscribed && !feed.isLocalFeed());
        viewBinding.toolbar.getMenu().findItem(R.id.visit_website_item).setVisible(isSubscribed
                && feed.getLink() != null
                && IntentUtils.isCallable(getContext(), new Intent(Intent.ACTION_VIEW, Uri.parse(feed.getLink()))));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (feed == null) {
            EventBus.getDefault().post(new MessageEvent(getString(R.string.please_wait_for_data)));
            return false;
        }
        if (item.getItemId() == R.id.visit_website_item) {
            IntentUtils.openInBrowser(getContext(), feed.getLink());
        } else if (item.getItemId() == R.id.share_item) {
            ShareUtils.shareFeedLink(getContext(), feed);
        } else {
            return false;
        }
        return true;
    }

}
