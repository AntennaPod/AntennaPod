package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;
import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.DefaultActionButtonCallback;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.feed.FeedMedia;
import de.danoeh.antennapod.preferences.UserPreferences;
import de.danoeh.antennapod.storage.DBTasks;
import de.danoeh.antennapod.storage.DBWriter;
import de.danoeh.antennapod.storage.DownloadRequestException;
import de.danoeh.antennapod.storage.DownloadRequester;
import de.danoeh.antennapod.util.QueueAccess;
import de.danoeh.antennapod.util.ShownotesProvider;
import de.danoeh.antennapod.util.menuhandler.FeedItemMenuHandler;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * Shows information about a specific FeedItem and provides actions like playing, downloading, etc.
 */
public class FeedItemDialog extends Dialog {
    private static final String TAG = "FeedItemDialog";

    private FeedItem item;
    private QueueAccess queue;

    private View header;
    private WebView webvDescription;
    private ImageButton butAction1;
    private ImageButton butAction2;
    private ImageButton butMore;
    private PopupMenu popupMenu;

    public FeedItemDialog(Context context, FeedItem item, QueueAccess queue) {
        super(context);
        if (item == null) throw new IllegalArgumentException("item = null");
        if (queue == null) throw new IllegalArgumentException("queue = null");
        this.item = item;
        this.queue = queue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feeditem_dialog);

        header = findViewById(R.id.header);
        webvDescription = (WebView) findViewById(R.id.webview);
        butAction1 = (ImageButton) findViewById(R.id.butAction1);
        butAction2 = (ImageButton) findViewById(R.id.butAction2);
        butMore = (ImageButton) findViewById(R.id.butMoreActions);
        popupMenu = new PopupMenu(getContext(), butMore);

        webvDescription.setWebViewClient(new WebViewClient());
        setTitle(item.getTitle());

        if (UserPreferences.getTheme() == R.style.Theme_AntennaPod_Dark) {
            if (Build.VERSION.SDK_INT >= 11
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                webvDescription.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            webvDescription.setBackgroundColor(getContext().getResources().getColor(
                    R.color.black));
        }
        webvDescription.getSettings().setUseWideViewPort(false);
        webvDescription.getSettings().setLayoutAlgorithm(
                WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webvDescription.getSettings().setLoadWithOverviewMode(true);
        webvDescription.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    getContext().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        });

        loadDescriptionWebview(item);

        butAction1.setOnClickListener(new View.OnClickListener() {
                                          DefaultActionButtonCallback actionButtonCallback = new DefaultActionButtonCallback(getContext());

                                          @Override

                                          public void onClick(View v) {
                                              FeedMedia media = item.getMedia();
                                              if (media == null) {
                                                  return;
                                              }
                                              actionButtonCallback.onActionButtonPressed(item);

                                          }
                                      }
        );

        butAction2.setOnClickListener(new View.OnClickListener()

                                      {
                                          @Override
                                          public void onClick(View v) {
                                              FeedMedia media = item.getMedia();
                                              if (media == null) {
                                                  return;
                                              }

                                              if (!media.isDownloaded()) {
                                                  DBTasks.playMedia(getContext(), media, true, true, true);
                                                  dismiss();
                                              } else {
                                                  DBWriter.deleteFeedMediaOfItem(getContext(), media.getId());
                                              }
                                          }
                                      }
        );

        butMore.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           popupMenu.getMenu().clear();
                                           popupMenu.inflate(R.menu.feeditem_dialog);
                                           FeedItemMenuHandler.onPrepareMenu(popupMenuInterface, item, true, queue);
                                           popupMenu.show();
                                       }
                                   }
        );

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                 @Override
                                                 public boolean onMenuItemClick(MenuItem menuItem) {

                                                     try {
                                                         return FeedItemMenuHandler.onMenuItemClicked(getContext(), menuItem.getItemId(), item);
                                                     } catch (DownloadRequestException e) {
                                                         e.printStackTrace();
                                                         Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                                         return true;
                                                     }
                                                 }
                                             }
        );

        updateMenuAppearance();
    }

    private final FeedItemMenuHandler.MenuInterface popupMenuInterface = new FeedItemMenuHandler.MenuInterface() {
        @Override
        public void setItemVisibility(int id, boolean visible) {
            MenuItem item = popupMenu.getMenu().findItem(id);
            if (item != null) {
                item.setVisible(visible);
            }
        }
    };

    public void updateMenuAppearance() {
        if (item == null || queue == null) {
            Log.w(TAG, "UpdateMenuAppearance called while item or queue was null");
            return;
        }
        FeedMedia media = item.getMedia();
        if (media == null) {
            header.setVisibility(View.GONE);
        } else {
            header.setVisibility(View.VISIBLE);
            boolean isDownloading = DownloadRequester.getInstance().isDownloadingFile(media);
            TypedArray drawables = getContext().obtainStyledAttributes(new int[]{R.attr.av_play,
                    R.attr.av_download, R.attr.action_stream, R.attr.content_discard, R.attr.navigation_cancel});

            if (!media.isDownloaded()) {
                butAction2.setImageDrawable(drawables.getDrawable(2));
                butAction2.setContentDescription(getContext().getString(R.string.stream_label));
            } else {
                butAction2.setImageDrawable(drawables.getDrawable(3));
                butAction2.setContentDescription(getContext().getString(R.string.remove_episode_lable));
            }

            if (isDownloading) {
                butAction1.setImageDrawable(drawables.getDrawable(4));
                butAction1.setContentDescription(getContext().getString(R.string.cancel_download_label));
            } else if (media.isDownloaded()) {
                butAction1.setImageDrawable(drawables.getDrawable(0));
                butAction1.setContentDescription(getContext().getString(R.string.play_label));
            } else {
                butAction1.setImageDrawable(drawables.getDrawable(1));
                butAction1.setContentDescription(getContext().getString(R.string.download_label));
            }

            drawables.recycle();
        }
    }


    private void loadDescriptionWebview(final ShownotesProvider shownotesProvider) {
        AsyncTask<Void, Void, Void> loadTask = new AsyncTask<Void, Void, Void>() {
            String data;


            private String applyWebviewStyle(String textColor, String data) {
                final String WEBVIEW_STYLE = "<html><head><style type=\"text/css\"> * { color: %s; font-family: Helvetica; line-height: 1.3em; font-size: 11pt; } a { font-style: normal; text-decoration: none; font-weight: normal; color: #00A8DF; } img { display: block; margin: 10 auto; max-width: %s; height: auto; } body { margin: %dpx %dpx %dpx %dpx; }</style></head><body>%s</body></html>";
                final int pageMargin = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8, getContext().getResources()
                                .getDisplayMetrics()
                );
                return String.format(WEBVIEW_STYLE, textColor, "100%", pageMargin,
                        pageMargin, pageMargin, pageMargin, data);
            }


            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                // /webvDescription.loadData(url, "text/html", "utf-8");
                if (FeedItemDialog.this.isShowing() && webvDescription != null) {
                    webvDescription.loadDataWithBaseURL(null, data, "text/html",
                            "utf-8", "about:blank");
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Webview loaded");
                }
            }


            @Override
            protected Void doInBackground(Void... params) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Loading Webview");
                try {
                    Callable<String> shownotesLoadTask = shownotesProvider.loadShownotes();
                    final String shownotes = shownotesLoadTask.call();

                    data = StringEscapeUtils.unescapeHtml4(shownotes);
                    TypedArray res = getContext()
                            .getTheme()
                            .obtainStyledAttributes(
                                    new int[]{android.R.attr.textColorPrimary});
                    int colorResource = res.getColor(0, 0);
                    String colorString = String.format("#%06X",
                            0xFFFFFF & colorResource);
                    Log.i(TAG, "text color: " + colorString);
                    res.recycle();
                    data = applyWebviewStyle(colorString, data);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        };
        loadTask.execute();
    }

    public void setItem(FeedItem item) {
        if (item == null) throw new IllegalArgumentException("item = null");
        this.item = item;
    }

    public void setItemFromCollection(Collection<FeedItem> items) {
        for (FeedItem item : items) {
            if (item.getId() == this.item.getId()) {
                setItem(item);
                break;
            }
        }
    }

    public void setQueue(QueueAccess queue) {
        if (queue == null) throw new IllegalArgumentException("queue = null");
        this.queue = queue;
    }

    public FeedItem getItem() {
        return item;
    }

    public QueueAccess getQueue() {
        return queue;
    }
}
