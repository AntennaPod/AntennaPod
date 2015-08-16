package de.danoeh.antennapod.core.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.danoeh.antennapod.core.event.FeedMediaEvent;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.util.NetworkUtils;
import de.greenrobot.event.EventBus;

public class FeedMediaSizeService extends IntentService {

    private final static String TAG = "FeedMediaSizeService";

    public FeedMediaSizeService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent()");
        if(false == NetworkUtils.isDownloadAllowed()) {
            return;
        }
        List<FeedMedia> list = DBReader.getFeedMediaUnknownSize(this);
        for (FeedMedia media : list) {
            Log.d(TAG, "Getting size currently " + media.getSize() + " for " + media.getDownload_url());
            if(false == NetworkUtils.isDownloadAllowed()) {
                return;
            }
            long size = Integer.MIN_VALUE;
            if (media.isDownloaded()) {
                File mediaFile = new File(media.getLocalMediaUrl());
                if(mediaFile.exists()) {
                    size = mediaFile.length();
                }
            } else if (false == media.checkedOnSizeButUnknown()) {
                // only query the network if we haven't already checked
                OkHttpClient client = AntennapodHttpClient.getHttpClient();
                Request.Builder httpReq = new Request.Builder()
                        .url(media.getDownload_url())
                        .header("Accept-Encoding", "identity")
                        .head();
                try {
                    Response response = client.newCall(httpReq.build()).execute();
                    if(response.isSuccessful()) {
                        String contentLength = response.header("Content-Length");
                        try {
                            size = Integer.parseInt(contentLength);
                        } catch(NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (size <= 0) {
                // they didn't tell us the size, but we don't want to keep querying on it
                media.setCheckedOnSizeButUnknown();
            } else {
                media.setSize(size);
            }
            Log.d(TAG, "Size now: " + media.getSize());
            DBWriter.setFeedMedia(this, media);
            EventBus.getDefault().post(FeedMediaEvent.update(media));

            // we don't want to stress the server too much
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
