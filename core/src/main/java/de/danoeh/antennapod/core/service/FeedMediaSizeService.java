package de.danoeh.antennapod.core.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.NetworkUtils;

public class FeedMediaSizeService extends IntentService {

    private final static String TAG = "FeedMediaSizeService";

    public FeedMediaSizeService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent()");
        if(false == NetworkUtils.networkAvailable(this)) {
            return;
        }
        List<FeedMedia> list = DBReader.getFeedMediaUnknownSize(this);
        for (FeedMedia media : list) {
            if(false == NetworkUtils.networkAvailable(this)) {
                return;
            }
            long size = Integer.MIN_VALUE;
            Log.d(TAG, media.getDownload_url());
            try {
                URL url = new URL(media.getDownload_url());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty( "Accept-Encoding", "" );
                conn.setRequestMethod("HEAD");
                size = conn.getContentLength();
                conn.disconnect();
            } catch (IOException e) {
                Log.d(TAG, media.getDownload_url());
                e.printStackTrace();
            }
            media.setSize(size);
            DBWriter.setFeedMedia(this, media);
        }
    }

}
