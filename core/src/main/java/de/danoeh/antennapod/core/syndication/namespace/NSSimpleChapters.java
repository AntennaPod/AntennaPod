package de.danoeh.antennapod.core.syndication.namespace;

import android.util.Log;

import org.xml.sax.Attributes;

import java.util.ArrayList;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.feed.Chapter;
import de.danoeh.antennapod.core.feed.SimpleChapter;
import de.danoeh.antennapod.core.syndication.handler.HandlerState;
import de.danoeh.antennapod.core.syndication.util.SyndDateUtils;

public class NSSimpleChapters extends Namespace {
    private static final String TAG = "NSSimpleChapters";

    public static final String NSTAG = "psc|sc";
    public static final String NSURI = "http://podlove.org/simple-chapters";

    public static final String CHAPTERS = "chapters";
    public static final String CHAPTER = "chapter";
    public static final String START = "start";
    public static final String TITLE = "title";
    public static final String HREF = "href";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state,
                                          Attributes attributes) {
        if (localName.equals(CHAPTERS)) {
            state.getCurrentItem().setChapters(new ArrayList<Chapter>());
        } else if (localName.equals(CHAPTER)) {
            try {
                state.getCurrentItem()
                        .getChapters()
                        .add(new SimpleChapter(SyndDateUtils
                                .parseTimeString(attributes.getValue(START)),
                                attributes.getValue(TITLE), state.getCurrentItem(),
                                attributes.getValue(HREF)));
            } catch (NumberFormatException e) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Unable to read chapter", e);
            }
        }

        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
    }

}
