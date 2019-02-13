package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
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
 * A timeline object needs a shownotesProvider from which the chapter information is retrieved and shownotes are generated.
 */
public class Timeline {
    private static final String TAG = "Timeline";

    private static final String WEBVIEW_STYLE = "@font-face { font-family: 'Roboto-Light'; src: url('file:///android_asset/Roboto-Light.ttf'); } * { color: %s; font-family: roboto-Light; font-size: 13pt; } a { font-style: normal; text-decoration: none; font-weight: normal; color: #00A8DF; } a.timecode { color: #669900; } img { display: block; margin: 10 auto; max-width: %s; height: auto; } body { margin: %dpx %dpx %dpx %dpx; }";


    private ShownotesProvider shownotesProvider;

    private final String noShownotesLabel;
    private final String colorPrimaryString;
    private final String colorSecondaryString;
    private final int pageMargin;

    public Timeline(Context context, ShownotesProvider shownotesProvider) {
        if (shownotesProvider == null) {
            throw new IllegalArgumentException("shownotesProvider = null");
        }
        this.shownotesProvider = shownotesProvider;

        noShownotesLabel = context.getString(R.string.no_shownotes_label);

        TypedArray res = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        @ColorInt int col = res.getColor(0, 0);
        colorPrimaryString = "rgba(" + Color.red(col) + "," + Color.green(col) + "," +
                Color.blue(col) + "," + (Color.alpha(col) / 255.0) + ")";
        res.recycle();
        res = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorSecondary});
        col = res.getColor(0, 0);
        colorSecondaryString = "rgba(" + Color.red(col) + "," + Color.green(col) + "," +
                Color.blue(col) + "," + (Color.alpha(col) / 255.0) + ")";
        res.recycle();

        pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                context.getResources().getDisplayMetrics()
        );
    }

    private static final Pattern TIMECODE_LINK_REGEX = Pattern.compile("antennapod://timecode/((\\d+))");
    private static final String TIMECODE_LINK = "<a class=\"timecode\" href=\"antennapod://timecode/%d\">%s</a>";
    private static final Pattern TIMECODE_REGEX = Pattern.compile("\\b(?:(?:([01]?\\d|2[0-3]):)?([0-5]?\\d):)?([0-5]?\\d)\\b");
    private static final Pattern LINE_BREAK_REGEX = Pattern.compile("<br */?>");


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

        if (TextUtils.isEmpty(shownotes)) {
            Log.d(TAG, "shownotesProvider contained no shownotes. Returning 'no shownotes' message");
            shownotes = "<html>" +
                    "<head>" +
                    "<style type='text/css'>" +
                    "html, body { margin: 0; padding: 0; width: 100%; height: 100%; } " +
                    "html { display: table; }" +
                    "body { display: table-cell; vertical-align: middle; text-align:center;" +
                    "-webkit-text-size-adjust: none; font-size: 87%; color: " + colorSecondaryString + ";} " +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<p>" + noShownotesLabel + "</p>" +
                    "</body>" +
                    "</html>";
            Log.d(TAG, "shownotes: " + shownotes);
            return shownotes;
        }

        // replace ASCII line breaks with HTML ones if shownotes don't contain HTML line breaks already
        if (!LINE_BREAK_REGEX.matcher(shownotes).find() && !shownotes.contains("<p>")) {
            shownotes = shownotes.replace("\n", "<br />");
        }

        Document document = Jsoup.parse(shownotes);

        // apply style
        String styleStr = String.format(Locale.getDefault(), WEBVIEW_STYLE, colorPrimaryString, "100%",
                pageMargin, pageMargin, pageMargin, pageMargin);
        document.head().appendElement("style").attr("type", "text/css").text(styleStr);

        // apply timecode links
        if (addTimecodes) {
            addTimecodes(document, playable);
        }

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


    public void setShownotesProvider(@NonNull ShownotesProvider shownotesProvider) {
        this.shownotesProvider = shownotesProvider;
    }

    private void addTimecodes(Document document, final Playable playable) {
        Elements elementsWithTimeCodes = document.body().getElementsMatchingOwnText(TIMECODE_REGEX);
        Log.d(TAG, "Recognized " + elementsWithTimeCodes.size() + " timecodes");

        // Assuming the timecodes are going to increase through the document loop through the
        // elements backwards so we can determine when/if we need to shift from HH:MM to MM:SS
        boolean useHourFormat = true;
        for (int i = elementsWithTimeCodes.size() - 1; i >= 0 ; i--) {
            Element element = elementsWithTimeCodes.get(i);
            Matcher matcherLong = TIMECODE_REGEX.matcher(element.html());

            // Get all matches and store in reverse order
            ArrayList<Pair<Boolean, String>> matches = new ArrayList<>();
            while (matcherLong.find()) {
                matches.add(0, new Pair<>(matcherLong.group(1) != null, matcherLong.group(0)));
            }

            // Now loop through the reversed matches and get the replacements. Store them in
            // non-reversed order.
            ArrayList<String> replacements = new ArrayList<>();
            for (Pair<Boolean, String> matchPair : matches) {
                boolean isLongFormat = matchPair.first;
                String group = matchPair.second;
                int time = isLongFormat
                            ? Converter.durationStringLongToMs(group)
                            : Converter.durationStringShortToMs(group, useHourFormat);

                String rep = group;
                if (playable == null) {
                    rep = createTimeLink(time, group);
                } else {
                    int duration = playable.getDuration();

                    if (duration > time) {
                        rep = createTimeLink(time, group);
                    } else if (!isLongFormat && useHourFormat) {

                        // The duration calculated in hours is too long and the timecode format is
                        // short. So try and see if it will work when treated as minutes.
                        time = Converter.durationStringShortToMs(group, false);

                        if (duration > time) {
                            // We have found the treating a short timecode as minutes works, do that
                            // from now on.
                            rep = createTimeLink(time, group);
                            useHourFormat = false;
                        }
                    }
                }

                replacements.add(0, rep);
            }

            // Now that we have all the replacements, replace.
            StringBuffer buffer = new StringBuffer();
            int index = 0;
            matcherLong.reset();
            while (matcherLong.find()) {
                matcherLong.appendReplacement(buffer, replacements.get(index));
                index++;
            }

            matcherLong.appendTail(buffer);
            element.html(buffer.toString());
        }
    }

    private String createTimeLink(int time, String group) {
        return String.format(Locale.getDefault(), TIMECODE_LINK, time, group);
    }
}
