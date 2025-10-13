package de.danoeh.antennapod.storage.database;

import de.danoeh.antennapod.model.feed.FeedItem;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedItemDuplicateGuesserPool {
    private final Map<String, List<FeedItem>> normalizedTitles = new HashMap<>();
    private final Map<String, FeedItem> downloadUrls = new HashMap<>();
    private final Map<String, FeedItem> identifiers = new HashMap<>();

    public FeedItemDuplicateGuesserPool(List<FeedItem> itemsList) {
        for (FeedItem item : itemsList) {
            add(item);
        }
    }

    public void add(FeedItem item) {
        String normalizedTitle = FeedItemDuplicateGuesser.canonicalizeTitle(item.getTitle());
        if (!normalizedTitles.containsKey(normalizedTitle)) {
            normalizedTitles.put(normalizedTitle, new java.util.ArrayList<>());
        }
        normalizedTitles.get(normalizedTitle).add(item);
        if (item.getMedia() != null && !StringUtils.isEmpty(item.getMedia().getStreamUrl())
                && !downloadUrls.containsKey(item.getMedia().getStreamUrl())) {
            downloadUrls.put(item.getMedia().getStreamUrl(), item);
        }
        if (item.getIdentifyingValue() != null && !identifiers.containsKey(item.getIdentifyingValue())) {
            identifiers.put(item.getIdentifyingValue(), item);
        }
    }

    public FeedItem guessDuplicate(FeedItem searchItem) {
        if (searchItem.getMedia() != null && !StringUtils.isEmpty(searchItem.getMedia().getStreamUrl())
                && downloadUrls.containsKey(searchItem.getMedia().getStreamUrl())) {
            return downloadUrls.get(searchItem.getMedia().getStreamUrl());
        }
        String normalizedTitle = FeedItemDuplicateGuesser.canonicalizeTitle(searchItem.getTitle());
        List<FeedItem> candidates = normalizedTitles.get(normalizedTitle);
        if (candidates == null) {
            return null;
        }
        for (FeedItem item : candidates) {
            if (FeedItemDuplicateGuesser.seemDuplicates(item, searchItem)) {
                return item;
            }
        }
        return null;
    }

    public FeedItem findById(FeedItem item) {
        if (identifiers.containsKey(item.getIdentifyingValue())) {
            return identifiers.get(item.getIdentifyingValue());
        }
        return null;
    }
}
