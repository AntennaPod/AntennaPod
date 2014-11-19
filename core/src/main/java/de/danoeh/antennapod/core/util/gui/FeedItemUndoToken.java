package de.danoeh.antennapod.core.util.gui;

import android.os.Parcel;
import android.os.Parcelable;
import de.danoeh.antennapod.core.feed.FeedItem;

/**
 * Used by an UndoBarController for saving a removed FeedItem
 */
public class FeedItemUndoToken implements Parcelable {
    private long itemId;
    private long feedId;
    private int position;

    public FeedItemUndoToken(FeedItem item, int position) {
        this.itemId = item.getId();
        this.feedId = item.getFeed().getId();
        this.position = position;
    }

    private FeedItemUndoToken(Parcel in) {
        itemId = in.readLong();
        feedId = in.readLong();
        position = in.readInt();
    }

    public static final Parcelable.Creator<FeedItemUndoToken> CREATOR = new Parcelable.Creator<FeedItemUndoToken>() {
        public FeedItemUndoToken createFromParcel(Parcel in) {
            return new FeedItemUndoToken(in);
        }

        public FeedItemUndoToken[] newArray(int size) {
            return new FeedItemUndoToken[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(itemId);
        out.writeLong(feedId);
        out.writeInt(position);
    }

    public long getFeedItemId() {
        return itemId;
    }

    public int getPosition() {
        return position;
    }
}

