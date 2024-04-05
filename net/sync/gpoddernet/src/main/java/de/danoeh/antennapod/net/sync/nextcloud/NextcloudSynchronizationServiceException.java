package de.danoeh.antennapod.net.sync.nextcloud;

import de.danoeh.antennapod.net.sync.serviceinterface.SyncServiceException;

public class NextcloudSynchronizationServiceException extends SyncServiceException {
    public NextcloudSynchronizationServiceException(Throwable e) {
        super(e);
    }
}
