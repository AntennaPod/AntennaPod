package de.danoeh.antennapod.core.sync;

import android.util.Log;

import androidx.collection.ArrayMap;
import androidx.core.util.Pair;

import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.net.sync.model.EpisodeAction;

public class EpisodeActionFilter {

    public static final String TAG = "EpisodeActionFilter";

    public static Map<Pair<String, String>, EpisodeAction> getRemoteActionsOverridingLocalActions(
            List<EpisodeAction> remoteActions,
            List<EpisodeAction> queuedEpisodeActions) {
        // make sure more recent local actions are not overwritten by older remote actions
        Map<Pair<String, String>, EpisodeAction> remoteActionsThatOverrideLocalActions = new ArrayMap<>();
        Map<Pair<String, String>, EpisodeAction> localMostRecentPlayActions =
                createUniqueLocalMostRecentPlayActions(queuedEpisodeActions);
        for (EpisodeAction remoteAction : remoteActions) {
            Pair<String, String> key = new Pair<>(remoteAction.getPodcast(), remoteAction.getEpisode());
            switch (remoteAction.getAction()) {
                case NEW:
                    remoteActionsThatOverrideLocalActions.put(key, remoteAction);
                    break;
                case DOWNLOAD:
                    break;
                case PLAY:
                    EpisodeAction localMostRecent = localMostRecentPlayActions.get(key);
                    if (secondActionOverridesFirstAction(remoteAction, localMostRecent)) {
                        break;
                    }
                    EpisodeAction remoteMostRecentAction = remoteActionsThatOverrideLocalActions.get(key);
                    if (secondActionOverridesFirstAction(remoteAction, remoteMostRecentAction)) {
                        break;
                    }
                    remoteActionsThatOverrideLocalActions.put(key, remoteAction);
                    break;
                case DELETE:
                    // NEVER EVER call DBWriter.deleteFeedMediaOfItem() here, leads to an infinite loop
                    break;
                default:
                    Log.e(TAG, "Unknown remoteAction: " + remoteAction);
                    break;
            }
        }

        return remoteActionsThatOverrideLocalActions;
    }

    private static Map<Pair<String, String>, EpisodeAction> createUniqueLocalMostRecentPlayActions(
            List<EpisodeAction> queuedEpisodeActions) {
        Map<Pair<String, String>, EpisodeAction> localMostRecentPlayAction;
        localMostRecentPlayAction = new ArrayMap<>();
        for (EpisodeAction action : queuedEpisodeActions) {
            Pair<String, String> key = new Pair<>(action.getPodcast(), action.getEpisode());
            EpisodeAction mostRecent = localMostRecentPlayAction.get(key);
            if (mostRecent == null || mostRecent.getTimestamp() == null) {
                localMostRecentPlayAction.put(key, action);
            } else if (mostRecent.getTimestamp().before(action.getTimestamp())) {
                localMostRecentPlayAction.put(key, action);
            }
        }
        return localMostRecentPlayAction;
    }

    private static boolean secondActionOverridesFirstAction(EpisodeAction firstAction,
                                                            EpisodeAction secondAction) {
        return secondAction != null
                && secondAction.getTimestamp() != null
                && secondAction.getTimestamp().after(firstAction.getTimestamp());
    }

}
