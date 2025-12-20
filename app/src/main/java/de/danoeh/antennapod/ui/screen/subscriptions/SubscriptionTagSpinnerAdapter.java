package de.danoeh.antennapod.ui.screen.subscriptions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubscriptionTagSpinnerAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private List<NavDrawerData.TagItem> tags = new ArrayList<>();
    private Map<Long, Integer> feedCounters;

    public SubscriptionTagSpinnerAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setTags(List<NavDrawerData.TagItem> tags, Map<Long, Integer> feedCounters) {
        this.tags = tags != null ? tags : new ArrayList<>();
        this.feedCounters = feedCounters;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tags.size();
    }

    @Override
    public NavDrawerData.TagItem getItem(int position) {
        return tags.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        }
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDisplayText(position));
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getDisplayText(position));
        return convertView;
    }

    private String getDisplayText(int position) {
        NavDrawerData.TagItem tag = tags.get(position);
        String title = tag.getTitle();
        
        // Use proper display names for special tags
        if (FeedPreferences.TAG_ROOT.equals(title)) {
            title = inflater.getContext().getString(R.string.tag_all);
        } else if (FeedPreferences.TAG_UNTAGGED.equals(title)) {
            title = inflater.getContext().getString(R.string.tag_untagged);
        }
        
        int feedCount = tag.getFeeds().size();
        int episodeCount = 0;
        if (feedCounters != null) {
            for (Feed feed : tag.getFeeds()) {
                Integer count = feedCounters.get(feed.getId());
                if (count != null) {
                    episodeCount += count;
                }
            }
        }
        return episodeCount > 0 ? title + " (" + feedCount + "/" + episodeCount + ")" : title + " (" + feedCount + ")";
    }

    public int getPositionForTag(String tagTitle) {
        for (int i = 0; i < tags.size(); i++) {
            if (tagTitle.equals(tags.get(i).getTitle())) {
                return i;
            }
        }
        return 0;
    }

    public String getTagAtPosition(int position) {
        if (position >= 0 && position < tags.size()) {
            return tags.get(position).getTitle();
        }
        return FeedPreferences.TAG_ROOT;
    }
}
