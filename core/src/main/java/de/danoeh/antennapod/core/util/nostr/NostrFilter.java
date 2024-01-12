package de.danoeh.antennapod.core.util.nostr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Defines Nostr filters according
 * to <a href="https://github.com/nostr-protocol/nips/blob/master/01.md#from-client-to-relay-sending-events-and-creating-subscriptions">NIP-01</a>.
 * <p>
 * The code has been trimmed down to the necessary portions(some fields are absent, since the fields are optional).
 */
public class NostrFilter {
    private static final String TAG = "NostrFilterCodec";

    private List<String> eventIds;
    private List<String> authors;
    private List<Integer> kindList;

    public static NostrFilter get(){
        return new NostrFilter();
    }

    public NostrFilter addAuthors(List<String> authors){
        this.authors = authors;
        return this;
    }

    public NostrFilter addKinds(List<Integer> kinds){
        this.kindList = kinds;
        return this;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<Integer> getKindList() {
        return kindList;
    }

    public String toJson() throws JSONException {
        JSONObject filterObject = new JSONObject();
        if (authors != null){
            JSONArray authorsArray = new JSONArray(authors);
            filterObject.put("authors", authorsArray);
        }
        if (kindList != null){
            JSONArray kindsArray = new JSONArray();
            for (Integer kind: kindList) {
                kindsArray.put(kind.intValue());
            }
            filterObject.put("kinds", kindsArray);
        }

        return filterObject.toString();
    }

}
