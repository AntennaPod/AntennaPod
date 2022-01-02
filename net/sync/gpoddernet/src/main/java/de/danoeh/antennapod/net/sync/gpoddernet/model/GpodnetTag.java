package de.danoeh.antennapod.net.sync.gpoddernet.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class GpodnetTag implements Parcelable  {

    private final String title;
    private final String tag;
    private final int usage;

    public GpodnetTag(@NonNull String title, @NonNull String tag, int usage) {
        this.title = title;
        this.tag = tag;
        this.usage = usage;
    }

    private GpodnetTag(Parcel in) {
        title = in.readString();
        tag = in.readString();
        usage = in.readInt();
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

    public static final Creator<GpodnetTag> CREATOR = new Creator<GpodnetTag>() {
        @Override
        public GpodnetTag createFromParcel(Parcel in) {
            return new GpodnetTag(in);
        }

        @Override
        public GpodnetTag[] newArray(int size) {
            return new GpodnetTag[size];
        }
    };

}
