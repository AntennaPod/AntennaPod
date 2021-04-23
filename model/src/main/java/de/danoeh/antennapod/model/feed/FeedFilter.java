package de.danoeh.antennapod.model.feed;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedFilter implements Serializable {
    private final String includeFilter;
    private final String excludeFilter;

    public FeedFilter() {
        this("", "");
    }

    public FeedFilter(String includeFilter, String excludeFilter) {
        // We're storing the strings and not the parsed terms because
        // 1. It's easier to show the user exactly what they typed in this way
        //    (we don't have to recreate it)
        // 2. We don't know if we'll actually be asked to parse anything anyways.
        this.includeFilter = includeFilter;
        this.excludeFilter = excludeFilter;
    }

    /**
     * Parses the text in to a list of single words or quoted strings.
     * Example: "One "Two Three"" returns ["One", "Two Three"]
     * @param filter string to parse in to terms
     * @return list of terms
     */
    private List<String> parseTerms(String filter) {
        // from http://stackoverflow.com/questions/7804335/split-string-on-spaces-in-java-except-if-between-quotes-i-e-treat-hello-wor
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(filter);
        while (m.find()) {
            list.add(m.group(1).replace("\"", ""));
        }
        return list;
    }

    /**
     * @param item
     * @return true if the item should be downloaded
     */
    public boolean shouldAutoDownload(FeedItem item) {

        List<String> includeTerms = parseTerms(includeFilter);
        List<String> excludeTerms = parseTerms(excludeFilter);

        if (includeTerms.size() == 0 && excludeTerms.size() == 0) {
            // nothing has been specified, so include everything
            return true;
        }

        // check using lowercase so the users don't have to worry about case.
        String title = item.getTitle().toLowerCase(Locale.getDefault());

        // if it's explicitly excluded, it shouldn't be autodownloaded
        // even if it has include terms
        for (String term : excludeTerms) {
            if (title.contains(term.trim().toLowerCase(Locale.getDefault()))) {
                return false;
            }
        }

        for (String term : includeTerms) {
            if (title.contains(term.trim().toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }

        // now's the tricky bit
        // if they haven't set an include filter, but they have set an exclude filter
        // default to including, but if they've set both, then exclude
        if (!hasIncludeFilter() && hasExcludeFilter()) {
            return true;
        }

        return false;
    }

    public String getIncludeFilter() {
        return includeFilter;
    }

    public String getExcludeFilter() {
        return excludeFilter;
    }

    /**
     * @return true if only include is set
     */
    public boolean includeOnly() {
        return hasIncludeFilter() && !hasExcludeFilter();
    }

    /**
     * @return true if only exclude is set
     */
    public boolean excludeOnly() {
        return hasExcludeFilter() && !hasIncludeFilter();
    }

    public boolean hasIncludeFilter() {
        return includeFilter.length() > 0;
    }

    public boolean hasExcludeFilter() {
        return excludeFilter.length() > 0;
    }
}
