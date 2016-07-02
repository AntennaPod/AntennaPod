package de.danoeh.antennapod.activity;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.StatisticsListAdapter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.Converter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Displays the 'statistics' screen
 */
public class StatisticsActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener {

    private static final String TAG = StatisticsActivity.class.getSimpleName();

    private Subscription subscription;
    private TextView totalTimeTextView;
    private ListView feedStatisticsList;
    private ProgressBar progressBar;
    private StatisticsListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.statistics_activity);

        totalTimeTextView = (TextView) findViewById(R.id.total_time);
        feedStatisticsList = (ListView) findViewById(R.id.statistics_list);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        listAdapter = new StatisticsListAdapter(this);
        feedStatisticsList.setAdapter(listAdapter);
        feedStatisticsList.setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        progressBar.setVisibility(View.VISIBLE);
        totalTimeTextView.setVisibility(View.GONE);
        feedStatisticsList.setVisibility(View.GONE);
        loadStats();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void loadStats() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        subscription = Observable.fromCallable(() -> DBReader.getStatistics())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        totalTimeTextView.setText(Converter
                                .shortLocalizedDuration(this, result.totalTime));
                        listAdapter.update(result.feedTime);
                        progressBar.setVisibility(View.GONE);
                        totalTimeTextView.setVisibility(View.VISIBLE);
                        feedStatisticsList.setVisibility(View.VISIBLE);
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DBReader.StatisticsItem stats = listAdapter.getItem(position);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(stats.feed.getTitle());
        dialog.setMessage(getString(R.string.statistics_details_dialog,
                stats.episodesStarted,
                stats.episodes,
                Converter.shortLocalizedDuration(this, stats.timePlayed),
                Converter.shortLocalizedDuration(this, stats.time)));
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }
}
