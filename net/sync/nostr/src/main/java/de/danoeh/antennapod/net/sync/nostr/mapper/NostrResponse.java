package de.danoeh.antennapod.net.sync.nostr.mapper;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import de.danoeh.antennapod.net.sync.nostr.model.NostrEvent;

public class NostrResponse {

    public static NostrEvent eventFrom(String responseJson) throws JSONException {
        JSONArray eventMessageArray = new JSONArray(responseJson);
        String eventJson = eventMessageArray.getString(2);
        return NostrEvent.fromJson(eventJson);

    }

    public static String relayClosedMessageFrom(String responseJson) throws JSONException {
        JSONArray closeMessageArray = new JSONArray(responseJson);
        String closeMarker = closeMessageArray.getString(0);
        String closeMessageContent = closeMessageArray.getString(2);
        String closeMessage = StringUtils.joinWith(": ", closeMarker, closeMessageContent);
        return closeMessage;
    }

    public static String relayNoticeFrom(String responseJson) throws JSONException {
        JSONArray noticeMessageArray = new JSONArray(responseJson);
        String noticeMarker = noticeMessageArray.getString(0);
        String noticeMessageContent = noticeMessageArray.getString(1);
        String closeMessage = StringUtils.joinWith(": ", noticeMarker, noticeMessageContent);
        return closeMessage;
    }

}
