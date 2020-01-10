package de.danoeh.antennapod.core.feed;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionFilter {
    public static final String STRING_ENABLE = "Enable";
    public static final String STRING_DISABLE = "Disable";
    public static final String STRING_ALWAYS = "Always";
    public static final String STRING_NEVER = "Never";
    public static final String STRING_GLOBAL_DEFAULT = "Global default";
    public static final String STRING_NO_FILTER = "No filter";

    public enum AutoDownload{
        ENABLE(STRING_ENABLE),
        DISABLE(STRING_DISABLE),
        NO_FILTER(STRING_NO_FILTER);
        private String autoDownloadString;
        AutoDownload(String autoDownloadStringValue) {
            autoDownloadString = autoDownloadStringValue;
        }

        @Override
        public String toString(){
            return autoDownloadString;
        }

        public static AutoDownload parse(String autoDownloadValue){
            switch (autoDownloadValue){
                case STRING_ENABLE: return AutoDownload.ENABLE;
                case STRING_DISABLE: return AutoDownload.DISABLE;
                case STRING_NO_FILTER: default: return AutoDownload.NO_FILTER;
            }
        }
    }
    public enum KeepUpdated{
        ENABLE(STRING_ENABLE),
        DISABLE(STRING_DISABLE),
        NO_FILTER(STRING_NO_FILTER);
        private String keepUpdatedString;
        KeepUpdated(String keepUpdatedStringValue) {
            keepUpdatedString = keepUpdatedStringValue;
        }

        @Override
        public String toString(){
            return keepUpdatedString;
        }

        public static KeepUpdated parse(String keepUpdatedValue){
            switch (keepUpdatedValue){
                case STRING_ENABLE: return KeepUpdated.ENABLE;
                case STRING_DISABLE: return KeepUpdated.DISABLE;
                case STRING_NO_FILTER: default: return KeepUpdated.NO_FILTER;
            }
        }
    }
    public enum AutoDelete{
        ALWAYS(STRING_ALWAYS),
        NEVER(STRING_NEVER),
        GLOBAL_DEFAULT(STRING_GLOBAL_DEFAULT),
        NO_FILTER(STRING_NO_FILTER);

        private String autoDeleteString;
        AutoDelete(String autoDeleteStringValue) {

            autoDeleteString = autoDeleteStringValue;
        }
        @Override
        public String toString(){
            return autoDeleteString;
        }

        public static AutoDelete parse(String autoDeleteValue){
            switch (autoDeleteValue){
                case STRING_ALWAYS: return ALWAYS;
                case STRING_NEVER: return NEVER;
                case STRING_GLOBAL_DEFAULT: return GLOBAL_DEFAULT;
                case STRING_NO_FILTER: default: return AutoDelete.NO_FILTER;
            }
        }
    }
    public ArrayList<Feed> subscriptionFilter(List<Feed> mCurrentFeeds,
                                              AutoDownload autoDownload,
                                              KeepUpdated keepUpdated,
                                              AutoDelete autoDelete){
        ArrayList<Feed> filteredFeeds = new ArrayList<>();
        FeedPreferences.AutoDeleteAction tempAutoDelete = null;
        switch (autoDelete) {
            case ALWAYS:
                tempAutoDelete = FeedPreferences.AutoDeleteAction.YES;
                break;
            case NEVER:
                tempAutoDelete = FeedPreferences.AutoDeleteAction.NO;
                break;
            case GLOBAL_DEFAULT:
                tempAutoDelete = FeedPreferences.AutoDeleteAction.GLOBAL;
                break;
        }
        boolean autoDownloadValue = autoDownload.equals(AutoDownload.ENABLE);
        boolean keepUpdatedValue = keepUpdated.equals(KeepUpdated.ENABLE);
        for(Feed feed: mCurrentFeeds){
            if(!autoDownload.equals(AutoDownload.NO_FILTER)&&
                    autoDownloadValue != feed.getPreferences().getAutoDownload())  {
                // not adding current feed as its auto Download value does not match to what user choose
                continue;
            }
            if(!keepUpdated.equals(KeepUpdated.NO_FILTER) &&
                    keepUpdatedValue != feed.getPreferences().getKeepUpdated()){
                // not adding current feed as its keep updated value does not match to what user choose
                continue;
            }

            if(!autoDelete.equals(AutoDelete.NO_FILTER)){
                FeedPreferences.AutoDeleteAction action = tempAutoDelete;

                FeedPreferences.AutoDeleteAction feedAction = feed.getPreferences().getAutoDeleteAction();
                if( feedAction!= action ){
                    // not adding current feed as its auto Download value does not match to what user choose
                    continue;
                }
            }
            filteredFeeds.add(feed);
        }
        return filteredFeeds;
    }
}
