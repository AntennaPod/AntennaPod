package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedItem;
import de.danoeh.antennapod.core.util.ShareUtils;

public class ShareDialog {

    private static final String TAG = "ShareDialog";
    private final Context ctx;
    private AlertDialog dialog;
    private List<String> shareDialogOptions;
    private String[] items;
    private FeedItem item;

    public ShareDialog(Context ctx, FeedItem item) {
        this.ctx = ctx;
        this.item = item;
        shareDialogOptions = new ArrayList<>();
    }

    public AlertDialog createDialog() {
        setupOptions();

        dialog = new AlertDialog.Builder(ctx)
                .setTitle(R.string.share_label)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (items[i].equals(ctx.getString(R.string.share_link_label))) {
                            ShareUtils.shareFeedItemLink(ctx, item);
                        } else if (items[i].equals(ctx.getString(R.string.share_item_url_label))) {
                            ShareUtils.shareFeedItemDownloadLink(ctx, item);
                        } else if (items[i].equals(ctx.getString(R.string.share_link_with_position_label))) {
                            ShareUtils.shareFeedItemLink(ctx, item, true);
                        } else if (items[i].equals(ctx.getString(R.string.share_item_url_with_position_label))) {
                            ShareUtils.shareFeedItemDownloadLink(ctx, item, true);
                        } else if (items[i].equals(ctx.getString(R.string.share_file_label))) {
                            ShareUtils.shareFeedItemFile(ctx, item.getMedia());
                        }
                    }
                })
                .create();

        return dialog;
    }

    private void setupOptions() {
        final boolean hasMedia = item.getMedia() != null;

        shareDialogOptions.add(ctx.getString(R.string.share_link_label));
        shareDialogOptions.add(ctx.getString(R.string.share_link_with_position_label));
        shareDialogOptions.add(ctx.getString(R.string.share_item_url_label));
        shareDialogOptions.add(ctx.getString(R.string.share_item_url_with_position_label));
        shareDialogOptions.add(ctx.getString(R.string.share_file_label));

        if (!ShareUtils.hasLinkToShare(item)) {
            shareDialogOptions.remove(ctx.getString(R.string.share_link_label));
            shareDialogOptions.remove(ctx.getString(R.string.share_link_with_position_label));
        }

        if (!hasMedia || item.getMedia().getDownload_url() == null) {
            shareDialogOptions.remove(ctx.getString(R.string.share_item_url_label));
            shareDialogOptions.remove(ctx.getString(R.string.share_item_url_with_position_label));
        }

        if (!hasMedia || item.getMedia().getPosition() <= 0) {
            shareDialogOptions.remove(ctx.getString(R.string.share_item_url_with_position_label));
            shareDialogOptions.remove(ctx.getString(R.string.share_link_with_position_label));
        }

        boolean fileDownloaded = hasMedia && item.getMedia().fileExists();
        if (!fileDownloaded) {
            shareDialogOptions.remove(ctx.getString(R.string.share_file_label));
        }

        //  preparing the resulting shareOptions for dialog
        items = new String[shareDialogOptions.size()];
        shareDialogOptions.toArray(items);
    }
}
