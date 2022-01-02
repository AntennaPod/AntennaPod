package de.danoeh.antennapod.net.sync.gpoddernet;

class GpodnetServiceBadStatusCodeException extends GpodnetServiceException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public GpodnetServiceBadStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
