package de.danoeh.antennapod.fragment;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.LangUtils;
import de.danoeh.antennapod.core.util.syndication.HtmlToPlainText;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import io.reactivex.Maybe;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Displays information about a feed.
 */
public class FeedInfoFragment extends Fragment {

    private static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String TAG = "FeedInfoActivity";

    private Feed feed;
    private Disposable disposable;
    private ImageView imgvCover;
    private TextView txtvTitle;
    private TextView txtvDescription;
    private TextView lblLanguage;
    private TextView txtvLanguage;
    private TextView lblAuthor;
    private TextView txtvAuthor;
    private TextView txtvUrl;
    private TextView txtvAuthorHeader;
    private ImageView imgvBackground;

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
            if(feed != null && feed.getDownload_url() != null) {
                String url = feed.getDownload_url();
                ClipData clipData = ClipData.newPlainText(url, url);
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(clipData);
                Toast t = Toast.makeText(getContext(), R.string.copied_url_msg, Toast.LENGTH_SHORT);
                t.show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).getSupportActionBar().setTitle(R.string.feed_info_label);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.feedinfo, null);
        setHasOptionsMenu(true);

        imgvCover = root.findViewById(R.id.imgvCover);
        txtvTitle = root.findViewById(R.id.txtvTitle);
        txtvAuthorHeader = root.findViewById(R.id.txtvAuthor);
        imgvBackground = root.findViewById(R.id.imgvBackground);
        root.findViewById(R.id.butShowInfo).setVisibility(View.INVISIBLE);
        root.findViewById(R.id.butShowSettings).setVisibility(View.INVISIBLE);
        // https://github.com/bumptech/glide/issues/529
        imgvBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));


        txtvDescription = root.findViewById(R.id.txtvDescription);
        lblLanguage = root.findViewById(R.id.lblLanguage);
        txtvLanguage = root.findViewById(R.id.txtvLanguage);
        lblAuthor = root.findViewById(R.id.lblAuthor);
        txtvAuthor = root.findViewById(R.id.txtvDetailsAuthor);
        txtvUrl = root.findViewById(R.id.txtvUrl);

        txtvUrl.setOnClickListener(copyUrlToClipboard);
        postponeEnterTransition();
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
                        }, error -> Log.d(TAG, Log.getStackTraceString(error)),
                        this::startPostponedEnterTransition);
    }

    private void showFeed() {
        Log.d(TAG, "Language is " + feed.getLanguage());
        Log.d(TAG, "Author is " + feed.getAuthor());
        Log.d(TAG, "URL is " + feed.getDownload_url());
        Glide.with(getContext())
                .load(feed.getImageLocation())
                .apply(new RequestOptions()
                        .placeholder(R.color.light_gray)
                        .error(R.color.light_gray)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .fitCenter()
                        .dontAnimate())
                .into(imgvCover);
        Glide.with(getContext())
                .load(feed.getImageLocation())
                .apply(new RequestOptions()
                        .placeholder(R.color.image_readability_tint)
                        .error(R.color.image_readability_tint)
                        .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                        .transform(new FastBlurTransformation())
                        .dontAnimate())
                .into(imgvBackground);

        txtvTitle.setText(feed.getTitle());

        String description = HtmlToPlainText.getPlainText(feed.getDescription());

        txtvDescription.setText(description);

        if (!TextUtils.isEmpty(feed.getAuthor())) {
            txtvAuthor.setText(feed.getAuthor());
            txtvAuthorHeader.setText(feed.getAuthor());
        } else {
            lblAuthor.setVisibility(View.GONE);
            txtvAuthor.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(feed.getLanguage())) {
            txtvLanguage.setText(LangUtils.getLanguageString(feed.getLanguage()));
        } else {
            lblLanguage.setVisibility(View.GONE);
            txtvLanguage.setVisibility(View.GONE);
        }
        txtvUrl.setText(feed.getDownload_url() + " {fa-paperclip}");
        Iconify.addIcons(txtvUrl);

        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.feedinfo, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.share_link_item).setVisible(feed != null && feed.getLink() != null);
        menu.findItem(R.id.visit_website_item).setVisible(feed != null && feed.getLink() != null &&
                IntentUtils.isCallable(getContext(), new Intent(Intent.ACTION_VIEW, Uri.parse(feed.getLink()))));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;
        try {
            handled = FeedMenuHandler.onOptionsItemClicked(getContext(), item, feed);
        } catch (DownloadRequestException e) {
            e.printStackTrace();
            DownloadRequestErrorDialogCreator.newRequestErrorDialog(getContext(), e.getMessage());
        }
        return handled || super.onOptionsItemSelected(item);
    }
}
