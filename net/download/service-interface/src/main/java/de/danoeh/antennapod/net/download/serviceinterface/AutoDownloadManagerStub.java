package de.danoeh.antennapod.net.download.serviceinterface;

import android.content.Context;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class AutoDownloadManagerStub extends AutoDownloadManager {

    @Override
    public Future<?> autodownloadUndownloadedItems(Context context) {
        FutureTask<?> task = new FutureTask<>(() -> null);
        task.run();
        return task;
    }

    @Override
    public void performAutoCleanup(Context context) {
    }
}
