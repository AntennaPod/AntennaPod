package de.danoeh.antennapod.activity;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.joanzapata.iconify.Iconify;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.dialog.DownloadRequestErrorDialogCreator;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.glide.FastBlurTransformation;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DownloadRequestException;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.LangUtils;
import de.danoeh.antennapod.core.util.syndication.HtmlToPlainText;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Displays information about a feed.
 */
public class FeedInfoActivity extends AppCompatActivity {

    public static final String EXTRA_FEED_ID = "de.danoeh.antennapod.extra.feedId";
    private static final String TAG = "FeedInfoActivity";
    private Feed feed;

    private ImageView imgvCover;
    private TextView txtvTitle;
    private TextView txtvDescription;
    private TextView lblLanguage;
    private TextView txtvLanguage;
    private TextView lblAuthor;
    private TextView txtvAuthor;
    private TextView txtvUrl;

    private Subscription subscription;


    @Nullable
    private final View.OnClickListener copyUrlToClipboard = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(feed != null && feed.getDownload_url() != null) {
                String url = feed.getDownload_url();
                ClipData clipData = ClipData.newPlainText(url, url);
                android.content.ClipboardManager cm = (android.content.ClipboardManager) FeedInfoActivity.this
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(clipData);
                Toast t = Toast.makeText(FeedInfoActivity.this, R.string.copied_url_msg, Toast.LENGTH_SHORT);
                t.show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedinfo);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long feedId = getIntent().getLongExtra(EXTRA_FEED_ID, -1);

        imgvCover = (ImageView) findViewById(R.id.imgvCover);
        txtvTitle = (TextView) findViewById(R.id.txtvTitle);
        TextView txtvAuthorHeader = (TextView) findViewById(R.id.txtvAuthor);
        ImageView imgvBackground = (ImageView) findViewById(R.id.imgvBackground);
        findViewById(R.id.butShowInfo).setVisibility(View.INVISIBLE);
        findViewById(R.id.butShowSettings).setVisibility(View.INVISIBLE);
        // https://github.com/bumptech/glide/issues/529
        imgvBackground.setColorFilter(new LightingColorFilter(0xff828282, 0x000000));


        txtvDescription = (TextView) findViewById(R.id.txtvDescription);
        lblLanguage = (TextView) findViewById(R.id.lblLanguage);
        txtvLanguage = (TextView) findViewById(R.id.txtvLanguage);
        lblAuthor = (TextView) findViewById(R.id.lblAuthor);
        txtvAuthor = (TextView) findViewById(R.id.txtvDetailsAuthor);
        txtvUrl = (TextView) findViewById(R.id.txtvUrl);

        txtvUrl.setOnClickListener(copyUrlToClipboard);

        subscription = Observable.fromCallable(()-> DBReader.getFeed(feedId))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result == null) {
                        Log.e(TAG, "Activity was started with invalid arguments");
                        finish();
                    }
                    feed = result;
                    Log.d(TAG, "Language is " + feed.getLanguage());
                    Log.d(TAG, "Author is " + feed.getAuthor());
                    Log.d(TAG, "URL is " + feed.getDownload_url());
                    Glide.with(FeedInfoActivity.this)
                            .load(feed.getImageLocation())
                            .placeholder(R.color.light_gray)
                            .error(R.color.light_gray)
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .fitCenter()
                            .dontAnimate()
                            .into(imgvCover);
                    Glide.with(FeedInfoActivity.this)
                            .load(feed.getImageLocation())
                            .placeholder(R.color.image_readability_tint)
                            .error(R.color.image_readability_tint)
                            .diskCacheStrategy(ApGlideSettings.AP_DISK_CACHE_STRATEGY)
                            .transform(new FastBlurTransformation(FeedInfoActivity.this))
                            .dontAnimate()
                            .into(imgvBackground);

                    txtvTitle.setText(feed.getTitle());

                    String description = feed.getDescription();
                    if(description != null) {
                        if(Feed.TYPE_ATOM1.equals(feed.getType())) {
                            HtmlToPlainText formatter = new HtmlToPlainText();
                            Document feedDescription = Jsoup.parse(feed.getDescription());
                            description = StringUtils.trim(formatter.getPlainText(feedDescription));
                        }
                    } else {
                        description = "";
                    }
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

                    supportInvalidateOptionsMenu();
                }, error -> {
                    Log.d(TAG, Log.getStackTraceString(error));
                    finish();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.feedinfo, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.support_item).setVisible(
                feed != null && feed.getPaymentLink() != null);
        menu.findItem(R.id.share_link_item).setVisible(feed != null && feed.getLink() != null);
        menu.findItem(R.id.visit_website_item).setVisible(feed != null && feed.getLink() != null &&
                IntentUtils.isCallable(this, new Intent(Intent.ACTION_VIEW, Uri.parse(feed.getLink()))));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                try {
                    return FeedMenuHandler.onOptionsItemClicked(this, item, feed);
                } catch (DownloadRequestException e) {
                    e.printStackTrace();
                    DownloadRequestErrorDialogCreator.newRequestErrorDialog(this,
                            e.getMessage());
                }
                return super.onOptionsItemSelected(item);
        }
    }
}
