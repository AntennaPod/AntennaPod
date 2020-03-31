package de.danoeh.antennapod.core.sync.gpoddernet.model;

import androidx.annotation.NonNull;

public class GpodnetDevice {

    private final String id;
    private final String caption;
    private final DeviceType type;
    private final int subscriptions;

    public GpodnetDevice(@NonNull String id,
                         String caption,
                         String type,
                         int subscriptions) {
        this.id = id;
        this.caption = caption;
        this.type = DeviceType.fromString(type);
        this.subscriptions = subscriptions;
    }

    @Override
    public String toString() {
        return "GpodnetDevice [id=" + id + ", caption=" + caption + ", type="
                + type + ", subscriptions=" + subscriptions + "]";
    }

    public enum DeviceType {
        DESKTOP, LAPTOP, MOBILE, SERVER, OTHER;

        static DeviceType fromString(String s) {
            if (s == null) {
                return OTHER;
            }

            switch (s) {
                case "desktop":
                    return DESKTOP;
                case "laptop":
                    return LAPTOP;
                case "mobile":
                    return MOBILE;
                case "server":
                    return SERVER;
                default:
                    return OTHER;
            }
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

    }

    public String getId() {
        return id;
    }

    public String getCaption() {
        return caption;
    }

    public DeviceType getType() {
        return type;
    }

    public int getSubscriptions() {
        return subscriptions;
    }

}
