package de.danoeh.antennapod.core.feed;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class FeedFunding {
public static final String FUNDING_ENTRIES_SEPARATOR = "\u001e";
    public static final String FUNDING_TITLE_SEPARATOR = "\u001f";

    public String url;
    public String content;

    public FeedFunding(String url, String content) {
        this.url = url;
        this.content = content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object obj) {
        if (! obj.getClass().equals(FeedFunding.class)) {
            return false;
        }

        FeedFunding funding = (FeedFunding) obj;
        if (url == null && funding.url == null && content == null && funding.content == null) {
            return true;
        }
        if (url != null && url.equals(funding.url) && content != null && content.equals(funding.content)) {
            return true;
        }
        return true;
    }

    public static ArrayList<FeedFunding> extractPaymentLinks(String payLinks) {
        if (StringUtils.isBlank(payLinks)) {
            return null;
        }
        // old format before we started with PodcastIndex funding tag
        ArrayList<FeedFunding> funding = new ArrayList<FeedFunding>();
        if (!payLinks.contains(FeedFunding.FUNDING_ENTRIES_SEPARATOR)
                && !payLinks.contains(FeedFunding.FUNDING_TITLE_SEPARATOR)) {
            funding.add(new FeedFunding(payLinks, ""));
            return funding;
        }
        String [] list = payLinks.split(FeedFunding.FUNDING_ENTRIES_SEPARATOR);
        if (list.length == 0) {
            return null;
        }

        for (String str : list) {
            String [] linkContent = str.split(FeedFunding.FUNDING_TITLE_SEPARATOR);
            if (StringUtils.isBlank(linkContent[0])) {
                continue;
            }
            String url = linkContent[0];
            String title = "";
            if (linkContent.length > 1 && ! StringUtils.isBlank(linkContent[1])) {
                title = linkContent[1];
            }
            funding.add(new FeedFunding(url, title));
        }
        return funding;
    }

    public static String getPaymentLinksAsString(ArrayList<FeedFunding> fundingList) {
        String result = "";
        if (fundingList == null) {
            return null;
        }
        for (FeedFunding fund : fundingList) {
            result += fund.url + FeedFunding.FUNDING_TITLE_SEPARATOR + fund.content;
            result += FeedFunding.FUNDING_ENTRIES_SEPARATOR;
        }
        result = StringUtils.removeEnd(result, FeedFunding.FUNDING_ENTRIES_SEPARATOR);
        return result;
    }

}