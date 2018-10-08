package de.danoeh.antennapod.core.gpoddernet.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class GpodnetDevice {

    @NonNull
    private final String id;
    private final String caption;
    @NonNull
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

    @NonNull
    @Override
    public String toString() {
        return "GpodnetDevice [id=" + id + ", caption=" + caption + ", type="
                + type + ", subscriptions=" + subscriptions + "]";
    }

    public enum DeviceType {
        DESKTOP, LAPTOP, MOBILE, SERVER, OTHER;

        @NonNull
        static DeviceType fromString(@Nullable String s) {
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

    @NonNull
    public String getId() {
        return id;
    }

    public String getCaption() {
        return caption;
    }

    @NonNull
    public DeviceType getType() {
        return type;
    }

    public int getSubscriptions() {
        return subscriptions;
    }

}
