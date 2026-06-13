package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.DBWriter;
import de.danoeh.antennapod.storage.database.FeedDatabaseWriter;
import de.danoeh.antennapod.databinding.OpmlSelectionBinding;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.model.feed.VolumeAdaptionSetting;
import de.danoeh.antennapod.storage.importexport.PortcastDocument;
import de.danoeh.antennapod.storage.importexport.PortcastEpisode;
import de.danoeh.antennapod.storage.importexport.PortcastQueueEntry;
import de.danoeh.antennapod.storage.importexport.PortcastReader;
import de.danoeh.antennapod.storage.importexport.PortcastSubscription;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.ToolbarActivity;
import de.danoeh.antennapod.ui.screen.preferences.ParentalControlDialog;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity for importing PortCast files (subscriptions, play state and queue).
 */
public class PortcastImportActivity extends ToolbarActivity {
    private static final String TAG = "PortcastImportActivity";
    @Nullable private Uri uri;
    private OpmlSelectionBinding viewBinding;
    private ArrayAdapter<String> listAdapter;
    private MenuItem selectAll;
    private MenuItem deselectAll;
    private PortcastDocument document;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        viewBinding = OpmlSelectionBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        viewBinding.feedlist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        viewBinding.feedlist.setOnItemClickListener((parent, view, position, id) -> {
            SparseBooleanArray checked = viewBinding.feedlist.getCheckedItemPositions();
            int checkedCount = 0;
            for (int i = 0; i < checked.size(); i++) {
                if (checked.valueAt(i)) {
                    checkedCount++;
                }
            }
            if (checkedCount == listAdapter.getCount()) {
                selectAll.setVisible(false);
                deselectAll.setVisible(true);
            } else {
                deselectAll.setVisible(false);
                selectAll.setVisible(true);
            }
        });
        viewBinding.butCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        viewBinding.butConfirm.setOnClickListener(v -> {
            if (UserPreferences.isParentalControlPasswordSet()
                    && UserPreferences.isParentalControlRequireSubscribeSet()) {
                ParentalControlDialog.show(this, this::doImport);
                return;
            }
            doImport();
        });

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith("/")) {
            uri = Uri.parse("file://" + uri.toString());
        } else {
            String extraText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            if (extraText != null) {
                uri = Uri.parse(extraText);
            }
        }
        importUri(uri);
    }

    private void doImport() {
        viewBinding.progressBar.setVisibility(View.VISIBLE);
        Completable.fromAction(() -> {
            SparseBooleanArray checked = viewBinding.feedlist.getCheckedItemPositions();
            for (int i = 0; i < checked.size(); i++) {
                if (!checked.valueAt(i)) {
                    continue;
                }
                importSubscription(document.getSubscriptions().get(checked.keyAt(i)));
            }
            importQueue();
            FeedUpdateManager.getInstance().runOnce(this);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            viewBinding.progressBar.setVisibility(View.GONE);
                            Intent intent = new Intent(PortcastImportActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        }, e -> {
                            e.printStackTrace();
                            viewBinding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        });
    }

    private void importSubscription(PortcastSubscription subscription) {
        if (TextUtils.isEmpty(subscription.getFeedUrl())) {
            return;
        }
        String title = !TextUtils.isEmpty(subscription.getTitle())
                ? subscription.getTitle() : "Unknown podcast";
        Feed feed = new Feed(subscription.getFeedUrl(), null, title);
        if (!TextUtils.isEmpty(subscription.getPodcastGuid())) {
            feed.setFeedIdentifier(subscription.getPodcastGuid());
        }
        feed.setItems(new ArrayList<>());
        Feed savedFeed = FeedDatabaseWriter.updateFeed(this, feed, false);
        if (savedFeed == null) {
            return;
        }
        applyPreferences(savedFeed, subscription);
        applyEpisodes(savedFeed, subscription);
    }

    private void applyPreferences(Feed savedFeed, PortcastSubscription subscription) {
        FeedPreferences preferences = new FeedPreferences(savedFeed.getId(),
                FeedPreferences.AutoDownloadSetting.GLOBAL, FeedPreferences.AutoDeleteAction.GLOBAL,
                VolumeAdaptionSetting.OFF, FeedPreferences.NewEpisodesAction.GLOBAL, null, null);
        preferences.getTags().addAll(subscription.getTags());
        if (subscription.getPlaybackSpeed() != null) {
            preferences.setFeedPlaybackSpeed(subscription.getPlaybackSpeed());
        }
        if (subscription.getSkipIntroSeconds() != null) {
            preferences.setFeedSkipIntro(subscription.getSkipIntroSeconds());
        }
        if (subscription.getSkipEndingSeconds() != null) {
            preferences.setFeedSkipEnding(subscription.getSkipEndingSeconds());
        }
        if (subscription.getAutoUpdate() != null) {
            preferences.setKeepUpdated(subscription.getAutoUpdate());
        }
        try {
            DBWriter.setFeedPreferences(preferences).get();
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void applyEpisodes(Feed savedFeed, PortcastSubscription subscription) {
        List<FeedItem> toPersist = new ArrayList<>();
        for (PortcastEpisode episode : subscription.getEpisodes()) {
            if (TextUtils.isEmpty(episode.getGuid()) && TextUtils.isEmpty(episode.getEnclosureUrl())) {
                continue;
            }
            FeedItem item = findExisting(savedFeed, episode);
            if (item == null) {
                item = new FeedItem();
                item.setItemIdentifier(episode.getGuid());
                item.setTitle(!TextUtils.isEmpty(episode.getGuid()) ? episode.getGuid() : episode.getEnclosureUrl());
                item.setPubDate(episode.getLastPlayedAt() != null ? episode.getLastPlayedAt() : new Date());
                item.setMedia(new FeedMedia(item, episode.getEnclosureUrl(), 0, null));
            }
            item.setFeed(savedFeed);
            boolean completed = PortcastDocument.STATUS_COMPLETED.equals(episode.getStatus());
            item.setPlayState(completed ? FeedItem.PLAYED : FeedItem.UNPLAYED);
            if (item.getMedia() != null) {
                item.getMedia().setPosition(completed ? 0 : episode.getPositionSeconds() * 1000);
                if (episode.getLastPlayedAt() != null) {
                    item.getMedia().setLastPlayedTimeHistory(episode.getLastPlayedAt());
                }
            }
            toPersist.add(item);
        }
        if (!toPersist.isEmpty()) {
            try {
                DBWriter.setItemList(toPersist).get();
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private FeedItem findExisting(Feed savedFeed, PortcastEpisode episode) {
        for (FeedItem item : savedFeed.getItems()) {
            if (!TextUtils.isEmpty(episode.getGuid()) && episode.getGuid().equals(item.getItemIdentifier())) {
                return item;
            }
            if (!TextUtils.isEmpty(episode.getEnclosureUrl()) && item.getMedia() != null
                    && episode.getEnclosureUrl().equals(item.getMedia().getDownloadUrl())) {
                return item;
            }
        }
        return null;
    }

    private void importQueue() {
        List<FeedItem> queueItems = new ArrayList<>();
        for (PortcastQueueEntry entry : document.getQueue()) {
            String guid = TextUtils.isEmpty(entry.getGuid()) ? null : entry.getGuid();
            String enclosureUrl = TextUtils.isEmpty(entry.getEnclosureUrl()) ? "" : entry.getEnclosureUrl();
            if (guid == null && enclosureUrl.isEmpty()) {
                continue;
            }
            FeedItem item = DBReader.getFeedItemByGuidOrEpisodeUrl(guid, enclosureUrl);
            if (item != null) {
                queueItems.add(item);
            }
        }
        if (!queueItems.isEmpty()) {
            try {
                DBReader.loadFeedDataOfFeedItemList(queueItems);
                DBWriter.addQueueItem(this, queueItems.toArray(new FeedItem[0])).get();
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }

    void importUri(@Nullable Uri uri) {
        if (uri == null) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.opml_import_error_no_file)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        this.uri = uri;
        startImport();
    }

    private List<String> getTitleList() {
        List<String> result = new ArrayList<>();
        if (document != null) {
            for (PortcastSubscription subscription : document.getSubscriptions()) {
                result.add(!TextUtils.isEmpty(subscription.getTitle())
                        ? subscription.getTitle() : subscription.getFeedUrl());
            }
        }
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.opml_selection_options, menu);
        selectAll = menu.findItem(R.id.select_all_item);
        deselectAll = menu.findItem(R.id.deselect_all_item);
        deselectAll.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.select_all_item) {
            selectAll.setVisible(false);
            selectAllItems(true);
            deselectAll.setVisible(true);
            return true;
        } else if (itemId == R.id.deselect_all_item) {
            deselectAll.setVisible(false);
            selectAllItems(false);
            selectAll.setVisible(true);
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
        }
        return false;
    }

    private void selectAllItems(boolean b) {
        for (int i = 0; i < viewBinding.feedlist.getCount(); i++) {
            viewBinding.feedlist.setItemChecked(i, b);
        }
    }

    /** Starts the import process. */
    private void startImport() {
        viewBinding.progressBar.setVisibility(View.VISIBLE);

        Observable.fromCallable(() -> {
            InputStream fileStream = getContentResolver().openInputStream(uri);
            BOMInputStream bomInputStream = new BOMInputStream(fileStream);
            ByteOrderMark bom = bomInputStream.getBOM();
            String charsetName = (bom == null) ? "UTF-8" : bom.getCharsetName();
            Reader reader = new InputStreamReader(bomInputStream, charsetName);
            PortcastReader portcastReader = new PortcastReader();
            PortcastDocument result = portcastReader.readDocument(reader);
            reader.close();
            return result;
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            viewBinding.progressBar.setVisibility(View.GONE);
                            Log.d(TAG, "Parsing was successful");
                            document = result;
                            listAdapter = new ArrayAdapter<>(PortcastImportActivity.this,
                                    android.R.layout.simple_list_item_multiple_choice,
                                    getTitleList());
                            viewBinding.feedlist.setAdapter(listAdapter);
                        }, e -> {
                            Log.d(TAG, Log.getStackTraceString(e));
                            viewBinding.progressBar.setVisibility(View.GONE);
                            MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this);
                            alert.setTitle(R.string.error_label);
                            alert.setMessage(getString(R.string.portcast_reader_error));
                            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> finish());
                            alert.show();
                        });
    }
}
