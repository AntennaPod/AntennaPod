package de.danoeh.antennapod.ui.echo.screen;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.echo.EchoConfig;
import de.danoeh.antennapod.ui.echo.R;
import de.danoeh.antennapod.ui.echo.background.RotatingSquaresBackground;
import de.danoeh.antennapod.ui.echo.databinding.SimpleEchoScreenBinding;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ThanksScreen extends EchoScreen {
    private final SimpleEchoScreenBinding viewBinding;

    public ThanksScreen(Context context, LayoutInflater layoutInflater) {
        super(context);
        viewBinding = SimpleEchoScreenBinding.inflate(layoutInflater);
        viewBinding.aboveLabel.setText("");
        viewBinding.largeLabel.setText(R.string.echo_thanks_large);

        viewBinding.smallLabel.setText(R.string.echo_thanks_now_favorite);
        viewBinding.backgroundImage.setImageDrawable(new RotatingSquaresBackground(context));
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
        if (statisticsResult.oldestDate < EchoConfig.jan1()) {
            String skeleton = DateFormat.getBestDateTimePattern(getEchoLanguage(), "MMMM yyyy");
            SimpleDateFormat dateFormat = new SimpleDateFormat(skeleton, getEchoLanguage());
            String dateFrom = dateFormat.format(new Date(statisticsResult.oldestDate));
            viewBinding.belowLabel.setText(context.getString(R.string.echo_thanks_we_are_glad_old, dateFrom));
        } else {
            viewBinding.belowLabel.setText(R.string.echo_thanks_we_are_glad_new);
        }
    }
}
