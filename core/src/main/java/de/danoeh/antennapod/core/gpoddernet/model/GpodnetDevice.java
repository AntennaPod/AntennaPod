package de.danoeh.antennapod.core.gpoddernet.model;

import org.apache.commons.lang3.Validate;

public class GpodnetDevice {

    private String id;
    private String caption;
    private DeviceType type;
    private int subscriptions;

    public GpodnetDevice(String id, String caption, String type,
                         int subscriptions) {
        Validate.notNull(id);

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

    public static enum DeviceType {
        DESKTOP, LAPTOP, MOBILE, SERVER, OTHER;

        static DeviceType fromString(String s) {
            if (s == null) {
                return OTHER;
            }

            if (s.equals("desktop")) {
                return DESKTOP;
            } else if (s.equals("laptop")) {
                return LAPTOP;
            } else if (s.equals("mobile")) {
                return MOBILE;
            } else if (s.equals("server")) {
                return SERVER;
            } else {
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
