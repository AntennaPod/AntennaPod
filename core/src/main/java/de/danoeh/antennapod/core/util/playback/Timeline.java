package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import android.util.TypedValue;

import org.apache.commons.lang3.Validate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.ShownotesProvider;

/**
 * Connects chapter information and shownotes of a shownotesProvider, for example by making it possible to use the
 * shownotes to navigate to another position in the podcast or by highlighting certain parts of the shownotesProvider's
 * shownotes.
 * <p/>
 * A timeline object needs a shownotesProvider from which the chapter information is retrieved and shownotes are generated.
 */
public class Timeline {
    private static final String TAG = "Timeline";

    private static final String WEBVIEW_STYLE = "@font-face { font-family: 'Roboto-Light'; src: url('file:///android_asset/Roboto-Light.ttf'); } * { color: %s; font-family: roboto-Light; font-size: 13pt; } a { font-style: normal; text-decoration: none; font-weight: normal; color: #00A8DF; } a.timecode { color: #669900; } img { display: block; margin: 10 auto; max-width: %s; height: auto; } body { margin: %dpx %dpx %dpx %dpx; }";


    private ShownotesProvider shownotesProvider;


    private final String colorString;
    private final int pageMargin;

    public Timeline(Context context, ShownotesProvider shownotesProvider) {
        if (shownotesProvider == null) throw new IllegalArgumentException("shownotesProvider = null");
        this.shownotesProvider = shownotesProvider;

        TypedArray res = context
                .getTheme()
                .obtainStyledAttributes(
                        new int[]{android.R.attr.textColorPrimary});
        int colorResource = res.getColor(0, 0);
        colorString = String.format("#%06X",
                0xFFFFFF & colorResource);
        res.recycle();

        pageMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources()
                        .getDisplayMetrics()
        );
    }

    private static final Pattern TIMECODE_LINK_REGEX = Pattern.compile("antennapod://timecode/((\\d+))");
    private static final String TIMECODE_LINK = "<a class=\"timecode\" href=\"antennapod://timecode/%d\">%s</a>";
    private static final Pattern TIMECODE_REGEX = Pattern.compile("\\b(?:(?:(([0-9][0-9])):))?(([0-9][0-9])):(([0-9][0-9]))\\b");

    /**
     * Applies an app-specific CSS stylesheet and adds timecode links (optional).
     * <p/>
     * This method does NOT change the original shownotes string of the shownotesProvider object and it should
     * also not be changed by the caller.
     *
     * @param addTimecodes True if this method should add timecode links
     * @return The processed HTML string.
     */
    public String processShownotes(final boolean addTimecodes) {
        final Playable playable = (shownotesProvider instanceof Playable) ? (Playable) shownotesProvider : null;

        // load shownotes

        String shownotes;
        try {
            shownotes = shownotesProvider.loadShownotes().call();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (shownotes == null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "shownotesProvider contained no shownotes. Returning empty string");
            return "";
        }

        Document document = Jsoup.parse(shownotes);

        // apply style
        String styleStr = String.format(WEBVIEW_STYLE, colorString, "100%", pageMargin,
                pageMargin, pageMargin, pageMargin);
        document.head().appendElement("style").attr("type", "text/css").text(styleStr);

        // apply timecode links
        if (addTimecodes) {
            Elements elementsWithTimeCodes = document.body().getElementsMatchingOwnText(TIMECODE_REGEX);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Recognized " + elementsWithTimeCodes.size() + " timecodes");
            for (Element element : elementsWithTimeCodes) {
                Matcher matcherLong = TIMECODE_REGEX.matcher(element.text());
                StringBuffer buffer = new StringBuffer();
                while (matcherLong.find()) {
                    String h = matcherLong.group(1);
                    String group = matcherLong.group(0);
                    int time = (h != null) ? Converter.durationStringLongToMs(group) :
                            Converter.durationStringShortToMs(group);

                    String rep;
                    if (playable == null || playable.getDuration() > time) {
                        rep = String.format(TIMECODE_LINK, time, group);
                    } else {
                        rep = group;
                    }
                    matcherLong.appendReplacement(buffer, rep);
                }
                matcherLong.appendTail(buffer);

                element.html(buffer.toString());
            }
        }

        Log.i(TAG, "Out: " + document.toString());
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
                    return Integer.valueOf(m.group(1));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }


    public void setShownotesProvider(ShownotesProvider shownotesProvider) {
        Validate.notNull(shownotesProvider);
        this.shownotesProvider = shownotesProvider;
    }
}
