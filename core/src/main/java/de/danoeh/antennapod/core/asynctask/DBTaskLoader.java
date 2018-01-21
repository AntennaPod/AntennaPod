package de.danoeh.antennapod.core.asynctask;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Subclass of AsyncTaskLoader that is made for loading data with one of the DB*-classes.
 * This class will provide a useful default implementation that would otherwise always be necessary when interacting
 * with the DB*-classes with an AsyncTaskLoader.
 */
abstract class DBTaskLoader<D> extends AsyncTaskLoader<D> {

    public DBTaskLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        cancelLoad();
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        // according to https://code.google.com/p/android/issues/detail?id=14944, this has to be called manually
        forceLoad();
    }
}
