package de.danoeh.antennapod.ui.echo.screen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.StatisticsItem;
import de.danoeh.antennapod.ui.echo.R;
import de.danoeh.antennapod.ui.echo.background.WavesBackground;
import de.danoeh.antennapod.ui.echo.databinding.SimpleEchoScreenBinding;

import java.util.ArrayList;

public class HoarderScreen extends EchoScreen {
    private final SimpleEchoScreenBinding viewBinding;

    public HoarderScreen(Context context, LayoutInflater layoutInflater) {
        super(context);
        viewBinding = SimpleEchoScreenBinding.inflate(layoutInflater);
        viewBinding.aboveLabel.setText(R.string.echo_hoarder_title);
        viewBinding.backgroundImage.setImageDrawable(new WavesBackground(context));
    }

    private void display(int playedActivePodcasts, int totalActivePodcasts, String randomUnplayedActivePodcast) {
        int percentagePlayed = (int) (100.0 * playedActivePodcasts / totalActivePodcasts);
        if (percentagePlayed < 25) {
            viewBinding.largeLabel.setText(R.string.echo_hoarder_emoji_cabinet);
            viewBinding.belowLabel.setText(R.string.echo_hoarder_subtitle_hoarder);
            viewBinding.smallLabel.setText(context.getString(R.string.echo_hoarder_comment_hoarder,
                    percentagePlayed, totalActivePodcasts));
        } else if (percentagePlayed < 75) {
            viewBinding.largeLabel.setText(R.string.echo_hoarder_emoji_check);
            viewBinding.belowLabel.setText(R.string.echo_hoarder_subtitle_medium);
            viewBinding.smallLabel.setText(context.getString(R.string.echo_hoarder_comment_medium,
                    percentagePlayed, totalActivePodcasts, randomUnplayedActivePodcast));
        } else {
            viewBinding.largeLabel.setText(R.string.echo_hoarder_emoji_clean);
            viewBinding.belowLabel.setText(R.string.echo_hoarder_subtitle_clean);
            viewBinding.smallLabel.setText(context.getString(R.string.echo_hoarder_comment_clean,
                    percentagePlayed, totalActivePodcasts));
        }
    }

    @Override
    public View getView() {
        return viewBinding.getRoot();
    }

    @Override
    public void postInvalidate() {
        viewBinding.backgroundImage.postInvalidate();
    }

    @Override
    public void startLoading(DBReader.StatisticsResult statisticsResult) {
        int totalActivePodcasts = 0;
        int playedActivePodcasts = 0;
        String randomUnplayedActivePodcast = "";
        ArrayList<String> unplayedActive = new ArrayList<>();
        for (StatisticsItem item : statisticsResult.feedTime) {
            if (item.feed.getPreferences().getKeepUpdated()) {
                totalActivePodcasts++;
                if (item.timePlayed > 0) {
                    playedActivePodcasts++;
                } else {
                    unplayedActive.add(item.feed.getTitle());
                }
            }
        }
        if (!unplayedActive.isEmpty()) {
            randomUnplayedActivePodcast = unplayedActive.get((int) (Math.random() * unplayedActive.size()));
        }
        display(playedActivePodcasts, totalActivePodcasts, randomUnplayedActivePodcast);
    }
}
