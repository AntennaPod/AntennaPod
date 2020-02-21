package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import de.danoeh.antennapod.core.feed.FeedItem;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.ShownotesProvider;

/**
 * Connects chapter information and shownotes of a shownotesProvider, for example by making it possible to use the
 * shownotes to navigate to another position in the podcast or by highlighting certain parts of the shownotesProvider's
 * shownotes.
 * <p/>
 * A timeline object needs a shownotesProvider from which the chapter information
 * is retrieved and shownotes are generated.
 */
public class Timeline {
    private static final String TAG = "Timeline";

    private static final Pattern TIMECODE_LINK_REGEX = Pattern.compile("antennapod://timecode/((\\d+))");
    private static final String TIMECODE_LINK = "<a class=\"timecode\" href=\"antennapod://timecode/%d\">%s</a>";
    private static final Pattern TIMECODE_REGEX = Pattern.compile("\\b((\\d+):)?(\\d+):(\\d{2})\\b");
    private static final Pattern LINE_BREAK_REGEX = Pattern.compile("<br */?>");

    private final ShownotesProvider shownotesProvider;
    private final String noShownotesLabel;
    private final String webviewStyle;

    public Timeline(Context context, ShownotesProvider shownotesProvider) {
        if (shownotesProvider == null) {
            throw new IllegalArgumentException("shownotesProvider = null");
        }
        this.shownotesProvider = shownotesProvider;

        noShownotesLabel = context.getString(R.string.no_shownotes_label);

        TypedArray res = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        @ColorInt int col = res.getColor(0, 0);
        final String colorPrimary = "rgba(" + Color.red(col) + "," + Color.green(col) + ","
                + Color.blue(col) + "," + (Color.alpha(col) / 255.0) + ")";
        res.recycle();
        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                context.getResources().getDisplayMetrics());
        String styleString = "";
        try {
            InputStream templateStream = context.getAssets().open("shownotes-style.css");
            styleString = IOUtils.toString(templateStream, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        webviewStyle = String.format(Locale.getDefault(), styleString, colorPrimary, margin, margin, margin, margin);
    }

    /**
     * Applies an app-specific CSS stylesheet and adds timecode links (optional).
     * <p/>
     * This method does NOT change the original shownotes string of the shownotesProvider object and it should
     * also not be changed by the caller.
     *
     * @return The processed HTML string.
     */
    @NonNull
    public String processShownotes() {
        String shownotes;
        try {
            shownotes = shownotesProvider.loadShownotes().call();
        } catch (Exception e) {
            Log.e(TAG, "processShownotes() - encounters exceptions unexpectedly in load, treat as if no shownotes.", e);
            shownotes = "";
        }

        if (TextUtils.isEmpty(shownotes)) {
            Log.d(TAG, "shownotesProvider contained no shownotes. Returning 'no shownotes' message");
            shownotes = "<html><head></head><body><p id='apNoShownotes'>" + noShownotesLabel + "</p></body></html>";
        }

        // replace ASCII line breaks with HTML ones if shownotes don't contain HTML line breaks already
        if (!LINE_BREAK_REGEX.matcher(shownotes).find() && !shownotes.contains("<p>")) {
            shownotes = shownotes.replace("\n", "<br />");
        }

        Document document = Jsoup.parse(shownotes);
        document.head().appendElement("style").attr("type", "text/css").text(webviewStyle);

        // apply timecode links
        addTimecodes(document);
        return document.toString();
    }

    /**
     * Returns true if the given link is a timecode link.
     */
    public static boolean isTimecodeLink(String link) {
        return link != null && link.matches(TIMECODE_LINK_REGEX.pattern());
    }

    /**
     * Returns the time in milliseconds that is attached to this link or -1
     * if the link is no valid timecode link.
     */
    public static int getTimecodeLinkTime(String link) {
        if (isTimecodeLink(link)) {
            Matcher m = TIMECODE_LINK_REGEX.matcher(link);

            try {
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    private void addTimecodes(Document document) {
        Elements elementsWithTimeCodes = document.body().getElementsMatchingOwnText(TIMECODE_REGEX);
        Log.d(TAG, "Recognized " + elementsWithTimeCodes.size() + " timecodes");

        if (elementsWithTimeCodes.size() == 0) {
            // No elements with timecodes
            return;
        }

        int playableDuration = Integer.MAX_VALUE;
        if (shownotesProvider instanceof Playable) {
            playableDuration = ((Playable) shownotesProvider).getDuration();
        } else if (shownotesProvider instanceof FeedItem && ((FeedItem) shownotesProvider).getMedia() != null) {
            playableDuration = ((FeedItem) shownotesProvider).getMedia().getDuration();
        }

        boolean useHourFormat = true;

        if (playableDuration != Integer.MAX_VALUE) {

            // We need to decide if we are going to treat short timecodes as HH:MM or MM:SS. To do
            // so we will parse all the short timecodes and see if they fit in the duration. If one
            // does not we will use MM:SS, otherwise all will be parsed as HH:MM.
            for (Element element : elementsWithTimeCodes) {
                Matcher matcherForElement = TIMECODE_REGEX.matcher(element.html());
                while (matcherForElement.find()) {

                    // We only want short timecodes right now.
                    if (matcherForElement.group(1) == null) {
                        int time = Converter.durationStringShortToMs(matcherForElement.group(0), true);

                        // If the parsed timecode is greater then the duration then we know we need to
                        // use the minute format so we are done.
                        if (time > playableDuration) {
                            useHourFormat = false;
                            break;
                        }
                    }
                }

                if (!useHourFormat) {
                    break;
                }
            }
        }

        for (Element element : elementsWithTimeCodes) {

            Matcher matcherForElement = TIMECODE_REGEX.matcher(element.html());
            StringBuffer buffer = new StringBuffer();

            while (matcherForElement.find()) {
                String group = matcherForElement.group(0);

                int time = matcherForElement.group(1) != null
                                        ? Converter.durationStringLongToMs(group)
                                        : Converter.durationStringShortToMs(group, useHourFormat);

                String replacementText = group;
                if (time < playableDuration) {
                    replacementText = String.format(Locale.getDefault(), TIMECODE_LINK, time, group);
                }

                matcherForElement.appendReplacement(buffer, replacementText);
            }

            matcherForElement.appendTail(buffer);
            element.html(buffer.toString());
        }
    }
}
