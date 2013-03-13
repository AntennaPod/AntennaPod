package de.danoeh.antennapod.asynctask;

import java.util.ArrayList;

import android.content.Context;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.opml.OpmlElement;
import de.danoeh.antennapod.util.googlereader.GoogleReader;

public abstract class GoogleReaderImportWorker extends AbstractImportWorker {

    private GoogleReader greader;

    public GoogleReaderImportWorker(Context context, GoogleReader greader) {
        super(context);
        this.greader = greader;
    }

    @Override
    protected ArrayList<OpmlElement> work() throws Exception {
        return greader.getListenSubscriptions();
    }

    @Override
    protected String getAlertMessage(Exception e) {
        // TODO Auto-generated method stub
        return context.getString(R.string.greader_fetching_error) + e.getMessage();
    }

    @Override
    protected String getInProgressMessage() {
        //TODO:
        return context.getString(R.string.fetching_greader_label);
    }
}
