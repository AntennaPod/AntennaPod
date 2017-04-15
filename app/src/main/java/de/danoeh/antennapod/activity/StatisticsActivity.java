package de.danoeh.antennapod.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
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
    private static final String PREF_NAME = "StatisticsActivityPrefs";
    private static final String PREF_COUNT_ALL = "countAll";

    private Subscription subscription;
    private TextView totalTimeTextView;
    private ListView feedStatisticsList;
    private ProgressBar progressBar;
    private StatisticsListAdapter listAdapter;
    private boolean countAll = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.statistics_activity);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        countAll = prefs.getBoolean(PREF_COUNT_ALL, false);

        totalTimeTextView = (TextView) findViewById(R.id.total_time);
        feedStatisticsList = (ListView) findViewById(R.id.statistics_list);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        listAdapter = new StatisticsListAdapter(this);
        listAdapter.setCountAll(countAll);
        feedStatisticsList.setAdapter(listAdapter);
        feedStatisticsList.setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatistics();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.statistics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.statistics_mode) {
            selectStatisticsMode();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void selectStatisticsMode() {
        View contentView = View.inflate(this, R.layout.statistics_mode_select_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(contentView);
        builder.setTitle(R.string.statistics_mode);

        if (countAll) {
            ((RadioButton) contentView.findViewById(R.id.statistics_mode_count_all)).setChecked(true);
        } else {
            ((RadioButton) contentView.findViewById(R.id.statistics_mode_normal)).setChecked(true);
        }

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            countAll = ((RadioButton) contentView.findViewById(R.id.statistics_mode_count_all)).isChecked();
            listAdapter.setCountAll(countAll);
            prefs.edit().putBoolean(PREF_COUNT_ALL, countAll).apply();
            refreshStatistics();
        });

        builder.show();
    }

    private void refreshStatistics() {
        progressBar.setVisibility(View.VISIBLE);
        totalTimeTextView.setVisibility(View.GONE);
        feedStatisticsList.setVisibility(View.GONE);
        loadStatistics();
    }

    private void loadStatistics() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        subscription = Observable.fromCallable(() -> DBReader.getStatistics(countAll))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null) {
                        totalTimeTextView.setText(Converter
                                .shortLocalizedDuration(this, countAll ? result.totalTimeCountAll : result.totalTime));
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
                countAll ? stats.episodesStartedIncludingMarked : stats.episodesStarted,
                stats.episodes,
                Converter.shortLocalizedDuration(this, countAll ?
                        stats.timePlayedCountAll : stats.timePlayed),
                Converter.shortLocalizedDuration(this, stats.time)));
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.show();
    }
}
