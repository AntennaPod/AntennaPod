package de.danoeh.antennapod.activity;

import java.util.ArrayList;

import de.danoeh.antennapod.core.export.opml.OpmlElement;

/**
 * Hold infos gathered by Ompl-Import
 * <p/>
 * Created with IntelliJ IDEA.
 * User: ligi
 * Date: 1/23/13
 * Time: 2:15 PM
 */
public class OpmlImportHolder {

    private OpmlImportHolder(){}

    private static ArrayList<OpmlElement> readElements;

    public static ArrayList<OpmlElement> getReadElements() {
        return readElements;
    }

    public static void setReadElements(ArrayList<OpmlElement> _readElements) {
        readElements = _readElements;
    }


}

