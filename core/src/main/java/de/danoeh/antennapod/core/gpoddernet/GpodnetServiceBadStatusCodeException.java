package de.danoeh.antennapod.core.gpoddernet;

class GpodnetServiceBadStatusCodeException extends GpodnetServiceException {
    private final int statusCode;

    public GpodnetServiceBadStatusCodeException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }


}
