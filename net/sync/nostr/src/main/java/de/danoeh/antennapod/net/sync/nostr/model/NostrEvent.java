package de.danoeh.antennapod.net.sync.nostr.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.danoeh.antennapod.net.sync.nostr.util.NostrUtil;

/**
 * Represents the Nostr Event structure as
 * defined in <a href="https://github.com/nostr-protocol/nips/blob/master/01.md#events-and-signatures">NIP-01</a>
 */

public class NostrEvent {

    private String eventId;
    private String pubkey;

    private Long creationDate;
    private int kind;
    private List<List<String>> tags;
    private String content;
    private String signature;

    public NostrEvent() {

    }

    public NostrEvent(String pubkey,
                      Long creationDate,
                      int kind,
                      List<List<String>> tags,
                      String content) {
        this.eventId = eventIdFrom(pubkey, creationDate, kind, tags, content);
        this.pubkey = pubkey;
        this.creationDate = creationDate;
        this.kind = kind;
        this.tags = tags;
        this.content = content;

    }

    public NostrEvent(
            String id,
            String pubkey,
            Long creationDate,
            int kind,
            List<List<String>> tags,
            String content,
            String signature
    ) {
        eventId = id;
        this.pubkey = pubkey;
        this.creationDate = creationDate;
        this.kind = kind;
        this.tags = tags;
        this.content = content;
        this.signature = signature;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPubkey() {
        return pubkey;
    }

    public Long getCreationDate() {
        return creationDate;
    }

    public List<List<String>> getTags() {
        return tags;
    }

    public String getContent() {
        return content;
    }

    private String eventIdFrom(
            String pubkey,
            Long creationDate,
            int kind,
            List<List<String>> tags,
            String content
    ) {
        JSONArray idGenerationStruct = new JSONArray();
        idGenerationStruct.put(0);
        idGenerationStruct.put(pubkey.toLowerCase(Locale.ENGLISH));
        idGenerationStruct.put(creationDate);
        idGenerationStruct.put(kind);
        idGenerationStruct.put(tags);
        idGenerationStruct.put(content);
        String json = idGenerationStruct.toString();
        String eventId = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] jsonHash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            eventId = NostrUtil.bytesToHex(jsonHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return eventId;
    }


    public NostrEvent fromJson(String eventJson) throws JSONException {
        JSONObject eventReader = new JSONObject(eventJson);
        String pubkey = eventReader.getString("pubkey");
        Long creationDate = eventReader.getLong("created_at");
        int kind = eventReader.getInt("kind");
        JSONArray tagListJSon = eventReader.getJSONArray("tags");
        List<List<String>> tags = new ArrayList<>();
        for (int listIndex = 0; listIndex < tagListJSon.length(); listIndex++) {
            JSONArray currentSubList = tagListJSon.getJSONArray(listIndex);
            int subListLength = currentSubList.length();
            if (subListLength == 1) {
                tags.add(Collections.singletonList(currentSubList.getString(0)));
            } else {
                List<String> tagArray = new ArrayList<>(subListLength);
                for (int i = 0; i < subListLength; i++) {
                    tagArray.add(currentSubList.getString(i));
                }
                tags.add(tagArray);
            }
        }
        String content = eventReader.getString("content");

        return new NostrEvent(pubkey, creationDate, kind, tags, content);
    }
}
