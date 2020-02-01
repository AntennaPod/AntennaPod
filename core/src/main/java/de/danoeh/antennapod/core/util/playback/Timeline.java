package de.danoeh.antennapod.core.util.playback;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

    private static final String WEBVIEW_STYLE =
            "@font-face {"
            + "font-family: 'Roboto-Light';"
            + "src: url('file:///android_asset/Roboto-Light.ttf');"
            + "}"
            + "* {"
            + "color: %s;"
            + "font-family: roboto-Light;"
            + "font-size: 13pt;"
            + "overflow-wrap: break-word;"
            + "}"
            + "a {"
            + "font-style: normal;"
            + "text-decoration: none;"
            + "font-weight: normal;"
            + "color: #00A8DF;"
            + "}"
            + "a.timecode {"
            + "color: #669900;"
            + "}"
            + "img, iframe {"
            + "display: block;"
            + "margin: 10 auto;"
            + "max-width: %s;"
            + "height: auto;"
            + "}"
            + "body {"
            + "margin: %dpx %dpx %dpx %dpx;"
            + "}";


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
    private static final Pattern TIMECODE_REGEX = Pattern.compile("\\b((\\d+):)?(\\d+):(\\d{2})\\b");
    private static final Pattern LINE_BREAK_REGEX = Pattern.compile("<br */?>");


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
        final Playable playable = (shownotesProvider instanceof Playable) ? (Playable) shownotesProvider : null;

        // load shownotes

        String shownotes;
        try {
            shownotes = shownotesProvider.loadShownotes().call();
        } catch (Exception e) {
            Log.e(TAG, "processShownotes() - encounters exceptions unexpectedly in load, treat as if no shownotes.", e);
            shownotes = "";
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
        addTimecodes(document, playable);
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

    private void addTimecodes(Document document, final Playable playable) {
        Elements elementsWithTimeCodes = document.body().getElementsMatchingOwnText(TIMECODE_REGEX);
        Log.d(TAG, "Recognized " + elementsWithTimeCodes.size() + " timecodes");

        if (elementsWithTimeCodes.size() == 0) {
            // No elements with timecodes
            return;
        }

        int playableDuration = playable == null ? Integer.MAX_VALUE : playable.getDuration();
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
