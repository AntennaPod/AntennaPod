package de.danoeh.antennapod.fragment;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBTasks;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.syndication.HtmlToPlainText;
import de.danoeh.antennapod.fragment.preferences.StatisticsFragment;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedFunding;
import de.danoeh.antennapod.view.ToolbarIconTintManager;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Displays information about a feed.
 */
public class FeedInfoFragment extends Fragment implements Toolbar.OnMenuItemClickListener {

    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String TAG = "FeedInfoActivity";
    private final ActivityResultLauncher<Uri> addLocalFolderLauncher =
            registerForActivityResult(new AddLocalFolder(), this::addLocalFolderResult);

    private Feed feed;
    private Disposable disposable;
    private ImageView imgvCover;
    private TextView txtvTitle;
    private TextView txtvDescription;
    private TextView txtvFundingUrl;
    private TextView lblSupport;
    private TextView txtvUrl;
    private TextView txtvAuthorHeader;
    private ImageView imgvBackground;
    private View infoContainer;
    private View header;
    private Toolbar toolbar;

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
            if (feed != null && feed.getDownload_url() != null) {
                String url = feed.getDownload_url();
                ClipData clipData = ClipData.newPlainText(url, url);
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(clipData);
                ((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.copied_url_msg, Snackbar.LENGTH_SHORT);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.feedinfo, null);
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.inflateMenu(R.menu.feedinfo);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        toolbar.setOnMenuItemClickListener(this);
        refreshToolbarState();

        AppBarLayout appBar = root.findViewById(R.id.appBar);
        CollapsingToolbarLayout collapsingToolbar = root.findViewById(R.id.collapsing_toolbar);
        ToolbarIconTintManager iconTintManager = new ToolbarIconTintManager(getContext(), toolbar, collapsingToolbar) {
            @Override
            protected void doTint(Context themedContext) {
                toolbar.getMenu().findItem(R.id.visit_website_item)
                        .setIcon(AppCompatDrawableManager.get().getDrawable(themedContext, R.drawable.ic_web));
                toolbar.getMenu().findItem(R.id.share_parent)
                        .setIcon(AppCompatDrawableManager.get().getDrawable(themedContext, R.drawable.ic_share));
            }
        };
        iconTintManager.updateTint();
        appBar.addOnOffsetChangedListener(iconTintManager);

        imgvCover = root.findViewById(R.id.imgvCover);
        txtvTitle = root.findViewById(R.id.txtvTitle);
        txtvAuthorHeader = root.findViewById(R.id.txtvAuthor);
        imgvBackground = root.findViewById(R.id.imgvBackground);
        header = root.findViewById(R.id.headerContainer);
        infoContainer = root.findViewById(R.id.infoContainer);
        root.findViewById(R.id.butShowInfo).setVisibility(View.INVISIBLE);
        root.findViewById(R.id.butShowSettings).setVisibility(View.INVISIBLE);
        // https://github.com/bumptech/glide/issues/529
        imgvBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));

        txtvDescription = root.findViewById(R.id.txtvDescription);
        txtvUrl = root.findViewById(R.id.txtvUrl);
        lblSupport = root.findViewById(R.id.lblSupport);
        txtvFundingUrl = root.findViewById(R.id.txtvFundingUrl);

        txtvUrl.setOnClickListener(copyUrlToClipboard);

        long feedId = getArguments().getLong(EXTRA_FEED_ID);
        getParentFragmentManager().beginTransaction().replace(R.id.statisticsFragmentContainer,
                        FeedStatisticsFragment.newInstance(feedId, false), "feed_statistics_fragment")
                .commitAllowingStateLoss();

        root.findViewById(R.id.btnvOpenStatistics).setOnClickListener(view -> {
            StatisticsFragment fragment = new StatisticsFragment();
            ((MainActivity) getActivity()).loadChildFragment(fragment, TransitionEffect.SLIDE);
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        long feedId = getArguments().getLong(EXTRA_FEED_ID);
        disposable = Maybe.create((MaybeOnSubscribe<Feed>) emitter -> {
            Feed feed = DBReader.getFeed(feedId);
            if (feed != null) {
                emitter.onSuccess(feed);
            } else {
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    feed = result;
                    showFeed();
                }, error -> Log.d(TAG, Log.getStackTraceString(error)), () -> { });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int horizontalSpacing = (int) getResources().getDimension(R.dimen.additional_horizontal_spacing);
        header.setPadding(horizontalSpacing, header.getPaddingTop(), horizontalSpacing, header.getPaddingBottom());
        infoContainer.setPadding(horizontalSpacing, infoContainer.getPaddingTop(),
                horizontalSpacing, infoContainer.getPaddingBottom());
    }

    private void showFeed() {
        Log.d(TAG, "Language is " + feed.getLanguage());
        Log.d(TAG, "Author is " + feed.getAuthor());
        Log.d(TAG, "URL is " + feed.getDownload_url());
        Glide.with(getContext())
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                .into(imgvCover);
        Glide.with(getContext())
                .load(feed.getImageUrl())
                .apply(new RequestOptions()
                        .placeholder(R.color.image_readability_tint)
                        .error(R.color.image_readability_tint)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .transform(new FastBlurTransformation())
                        .dontAnimate())
                .into(imgvBackground);

        txtvTitle.setText(feed.getTitle());
        txtvTitle.setMaxLines(6);

        String description = HtmlToPlainText.getPlainText(feed.getDescription());

        txtvDescription.setText(description);

        if (!TextUtils.isEmpty(feed.getAuthor())) {
            txtvAuthorHeader.setText(feed.getAuthor());
        }

        txtvUrl.setText(feed.getDownload_url() + " {fa-paperclip}");

        if (feed.getPaymentLinks() == null || feed.getPaymentLinks().size() == 0) {
            lblSupport.setVisibility(View.GONE);
            txtvFundingUrl.setVisibility(View.GONE);
        } else {
            lblSupport.setVisibility(View.VISIBLE);
            ArrayList<FeedFunding> fundingList = feed.getPaymentLinks();
            StringBuilder str = new StringBuilder();
            HashSet<String> seen = new HashSet<String>();
            for (FeedFunding funding : fundingList) {
                if (seen.contains(funding.url)) {
                    continue;
                }
                seen.add(funding.url);
                str.append(funding.content.isEmpty()
                        ? getContext().getResources().getString(R.string.support_podcast)
                        : funding.content).append(" ").append(funding.url);
                str.append("\n");
            }
            str = new StringBuilder(StringUtils.trim(str.toString()));
            txtvFundingUrl.setText(str.toString());
        }

        Iconify.addIcons(txtvUrl);
        refreshToolbarState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void refreshToolbarState() {
        boolean shareLinkVisible = feed != null && feed.getLink() != null;
        boolean downloadUrlVisible = feed != null && !feed.isLocalFeed();

        toolbar.getMenu().findItem(R.id.reconnect_local_folder).setVisible(feed != null && feed.isLocalFeed());
        toolbar.getMenu().findItem(R.id.share_download_url_item).setVisible(downloadUrlVisible);
        toolbar.getMenu().findItem(R.id.share_link_item).setVisible(shareLinkVisible);
        toolbar.getMenu().findItem(R.id.share_parent).setVisible(downloadUrlVisible || shareLinkVisible);
        toolbar.getMenu().findItem(R.id.visit_website_item).setVisible(feed != null && feed.getLink() != null
                && IntentUtils.isCallable(getContext(), new Intent(Intent.ACTION_VIEW, Uri.parse(feed.getLink()))));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (feed == null) {
            ((MainActivity) getActivity()).showSnackbarAbovePlayer(
                    R.string.please_wait_for_data, Toast.LENGTH_LONG);
            return false;
        }
        boolean handled = false;
        try {
            handled = FeedMenuHandler.onOptionsItemClicked(getContext(), item, feed);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(getContext(), e.getMessage());
        }

        if (item.getItemId() == R.id.reconnect_local_folder && Build.VERSION.SDK_INT >= 21) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setMessage(R.string.reconnect_local_folder_warning);
            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                try {
                    addLocalFolderLauncher.launch(null);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No activity found. Should never happen...");
                }
            });
            alert.setNegativeButton(android.R.string.cancel, null);
            alert.show();
            return true;
        }

        return handled;
    }

    private void addLocalFolderResult(final Uri uri) {
        if (uri == null) {
            return;
        }
        reconnectLocalFolder(uri);
    }

    private void reconnectLocalFolder(Uri uri) {
        if (Build.VERSION.SDK_INT < 21 || feed == null) {
            return;
        }

        Completable.fromAction(() -> {
            getActivity().getContentResolver()
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            DocumentFile documentFile = DocumentFile.fromTreeUri(getContext(), uri);
            if (documentFile == null) {
                throw new IllegalArgumentException("Unable to retrieve document tree");
            }
            feed.setDownload_url(Feed.PREFIX_LOCAL_FOLDER + uri.toString());
            DBTasks.updateFeed(getContext(), feed, true);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> ((MainActivity) getActivity())
                                .showSnackbarAbovePlayer(android.R.string.ok, Snackbar.LENGTH_SHORT),
                        error -> ((MainActivity) getActivity())
                                .showSnackbarAbovePlayer(error.getLocalizedMessage(), Snackbar.LENGTH_LONG));
    }

    private static class AddLocalFolder extends ActivityResultContracts.OpenDocumentTree {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @NonNull
        @Override
        public Intent createIntent(@NonNull final Context context, @Nullable final Uri input) {
            return super.createIntent(context, input)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }
}
