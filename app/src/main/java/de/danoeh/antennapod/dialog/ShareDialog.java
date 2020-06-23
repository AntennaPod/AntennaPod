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
    private final Context context;
    private AlertDialog dialog;
    private List<String> shareDialogOptions;
    private String[] arrayItems;
    private FeedItem item;

    public ShareDialog(Context context, FeedItem item) {
        this.context = context;
        this.item = item;
        shareDialogOptions = new ArrayList<>();
    }

    public AlertDialog createDialog() {
        setupOptions();

        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.share_label)
                .setItems(arrayItems, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if(arrayItems[which].equals(context.getString(R.string.share_link_label))){
                            ShareUtils.shareFeedItemLink(context, item);
                        } else if(arrayItems[which].equals(context.getString(R.string.share_item_url_label))) {
                            ShareUtils.shareFeedItemDownloadLink(context, item);
                        } else if(arrayItems[which].equals(context.getString(R.string.share_link_with_position_label))) {
                            ShareUtils.shareFeedItemLink(context, item, true);
                        } else if(arrayItems[which].equals(context.getString(R.string.share_item_url_with_position_label))) {
                            ShareUtils.shareFeedItemDownloadLink(context, item, true);
                        } else if(arrayItems[which].equals(context.getString(R.string.share_file_label))) {
                            ShareUtils.shareFeedItemFile(context, item.getMedia());
                        }
                    }
                })
                .create();

        return dialog;
    }

    private void setupOptions(){
        boolean hasMedia = item.getMedia() != null;

        shareDialogOptions.add(context.getString(R.string.share_link_label));
        shareDialogOptions.add(context.getString(R.string.share_link_with_position_label));
        shareDialogOptions.add(context.getString(R.string.share_item_url_label));
        shareDialogOptions.add(context.getString(R.string.share_item_url_with_position_label));
        shareDialogOptions.add(context.getString(R.string.share_file_label));

        if (!ShareUtils.hasLinkToShare(item)) {
            shareDialogOptions.remove(context.getString(R.string.share_link_label));
            shareDialogOptions.remove(context.getString(R.string.share_link_with_position_label));
        }

        if (!hasMedia || item.getMedia().getDownload_url() == null) {
            shareDialogOptions.remove(context.getString(R.string.share_item_url_label));
            shareDialogOptions.remove(context.getString(R.string.share_item_url_with_position_label));
        }

        if(!hasMedia || item.getMedia().getPosition() <= 0) {
            shareDialogOptions.remove(context.getString(R.string.share_item_url_with_position_label));
            shareDialogOptions.remove(context.getString(R.string.share_link_with_position_label));
        }

        boolean fileDownloaded = hasMedia && item.getMedia().fileExists();
        if(!fileDownloaded){
            shareDialogOptions.remove(context.getString(R.string.share_file_label));
        }

        //  preparing the resulting shareOptions for dialog
        arrayItems = new String[shareDialogOptions.size()];
        shareDialogOptions.toArray(arrayItems);
    }
}
