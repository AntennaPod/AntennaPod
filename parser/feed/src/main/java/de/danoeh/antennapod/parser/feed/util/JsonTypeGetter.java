package de.danoeh.antennapod.parser.feed.util;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.parser.feed.UnsupportedFeedtypeException;

public class JsonTypeGetter implements TypeGetter {
    @Override
    public TypeResolver.Type getType(Feed feed) throws UnsupportedFeedtypeException {
        InputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(feed.getFile_url());
            String jsonTxt = IOUtils.toString(fileInputStream, Charsets.UTF_8);
            fileInputStream.close();
            new JSONObject(jsonTxt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException jsonException) {
            throw new UnsupportedFeedtypeException(TypeResolver.Type.INVALID);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return TypeResolver.Type.JSON;
    }
}
