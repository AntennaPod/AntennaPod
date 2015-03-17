package de.danoeh.antennapod.core.gpoddernet.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.lang3.Validate;

public class GpodnetTag implements Parcelable  {

    private final String title;
    private final String tag;
    private final int usage;

    public GpodnetTag(String title, String tag, int usage) {
        Validate.notNull(title);
        Validate.notNull(tag);

        this.title = title;
        this.tag = tag;
        this.usage = usage;
    }

    public static GpodnetTag createFromParcel(Parcel in) {
        final String title = in.readString();
        final String tag = in.readString();
        final int usage = in.readInt();
        return new GpodnetTag(title, tag, usage);
    }

    @Override
    public String toString() {
        return "GpodnetTag [title="+title+", tag=" + tag + ", usage=" + usage + "]";
    }

    public String getTitle() {
        return title;
    }

    public String getTag() {
        return tag;
    }

    public int getUsage() {
        return usage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(tag);
        dest.writeInt(usage);
    }


}
