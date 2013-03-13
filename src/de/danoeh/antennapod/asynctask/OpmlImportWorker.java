package de.danoeh.antennapod.asynctask;

import java.io.*;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.opml.OpmlElement;
import de.danoeh.antennapod.opml.OpmlReader;

public abstract class OpmlImportWorker extends
		AbstractImportWorker {
    protected final String TAG = "OpmlImportWorker";
	Reader mReader;

    public OpmlImportWorker(Context context, Reader reader) {
        super(context);
        this.mReader=reader;
    }

	protected ArrayList<OpmlElement> work() throws Exception {
        if (mReader==null) {
            return null;
        }

		OpmlReader opmlReader = new OpmlReader();
		ArrayList<OpmlElement> result = opmlReader.readDocument(mReader);
        mReader.close();
        return result;
    }

    @Override
    protected String getInProgressMessage() {
        return context.getString(R.string.reading_opml_label);
    }
    
    @Override
    protected String getAlertMessage(Exception e) {
        return context.getString(R.string.opml_reader_error) + e.getMessage();
    }

}
