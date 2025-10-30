package de.danoeh.antennapod.ui.echo.screen;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.ui.echo.R;

import java.util.Locale;

public abstract class EchoScreen {
    protected final Context context;

    public EchoScreen(Context context) {
        this.context = context;
    }

    protected final Locale getEchoLanguage() {
        boolean hasTranslation = !context.getString(R.string.echo_listened_after_title)
                .equals(getLocalizedResources(Locale.US).getString(R.string.echo_listened_after_title));
        if (hasTranslation) {
            return Locale.getDefault();
        } else {
            return Locale.US;
        }
    }

    @NonNull
    protected Resources getLocalizedResources(Locale desiredLocale) {
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }

    public void postInvalidate() {
        // Do nothing by default
    }

    public abstract View getView();

    public void startLoading(DBReader.StatisticsResult statisticsResult) {
        // Do nothing by default
    }
}
