package de.danoeh.antennapod.net.sync.nostr;

import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.net.sync.model.EpisodeAction;
import de.danoeh.antennapod.net.sync.model.EpisodeActionChanges;
import de.danoeh.antennapod.net.sync.model.ISyncService;
import de.danoeh.antennapod.net.sync.model.SubscriptionChanges;
import de.danoeh.antennapod.net.sync.model.UploadChangesResponse;
import de.danoeh.antennapod.net.sync.nostr.mapper.NostrResponse;
import de.danoeh.antennapod.net.sync.nostr.model.NostrEvent;
import de.danoeh.antennapod.net.sync.nostr.util.NostrException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class NostrService implements ISyncService {
    public static final String TAG = "NostrService";
    private final String relayUrl;
    private final OkHttpClient nostrClient;
    private WebSocket webSocket;

    public NostrService(OkHttpClient httpClient, String relayUrl) {
        this.nostrClient = httpClient;
        this.relayUrl = relayUrl;
    }

    public List<NostrEvent> fetchEvents(String filter) throws NostrServiceException {
        ArrayList<NostrEvent> eventList = new ArrayList<>();

        WebSocketListener socketListener = new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "onOpen(): Connected to Relay -> " + relayUrl);
                webSocket.send(filter);

            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                try {
                    if (text.contains("EVENT")) {
                        NostrEvent event = NostrResponse.eventFrom(text);
                        eventList.add(event);
                    } else if (text.contains("NOTICE")) {
                        String noticeMessage = NostrResponse.relayNoticeFrom(text);
                        throw new NostrException(noticeMessage);
                    } else if (text.contains("CLOSE")) {
                        String closeMessage = NostrResponse.relayClosedMessageFrom(text);
                        throw new NostrException(closeMessage);
                    } else {
                        Log.e(TAG, "onMessage: " + text);
                    }
                } catch (JSONException | NostrException e) {
                    e.printStackTrace();
                    ExceptionUtils.rethrow(e);
                }
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                super.onClosed(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
                webSocket.cancel();
                stopSocket();
            }
        };

        this.webSocket = nostrClient.newWebSocket(
                new Request.Builder().url(relayUrl).build(),
                socketListener
        );

        return eventList;
    }

    private void stopSocket() {
        nostrClient.dispatcher().executorService().shutdown();
        this.webSocket = null;
    }

    @Override
    public void login() throws NostrServiceException {

    }

    @Override
    public SubscriptionChanges getSubscriptionChanges(long lastSync) throws NostrServiceException {
        return null;
    }

    @Override
    public UploadChangesResponse uploadSubscriptionChanges(List<String> addedFeeds, List<String> removedFeeds)
            throws NostrServiceException {
        return null;
    }

    @Override
    public EpisodeActionChanges getEpisodeActionChanges(long lastSync) throws NostrServiceException {
        return null;
    }

    @Override
    public UploadChangesResponse uploadEpisodeActions(List<EpisodeAction> queuedEpisodeActions)
            throws NostrServiceException {
        return null;
    }

    @Override
    public void logout() throws NostrServiceException {

    }

}
