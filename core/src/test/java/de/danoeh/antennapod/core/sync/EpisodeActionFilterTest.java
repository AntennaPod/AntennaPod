package de.danoeh.antennapod.core.sync;


import androidx.core.util.Pair;

import junit.framework.TestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.net.sync.model.EpisodeAction;


public class EpisodeActionFilterTest extends TestCase {

    EpisodeActionFilter episodeActionFilter = new EpisodeActionFilter();

    public void testGetRemoteActionsHappeningAfterLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");
        Date lateMorning = format.parse("2021-01-01 09:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(lateMorning)
                .position(20)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesLater = format.parse("2021-01-01 08:05:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = episodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertSame(1, uniqueList.size());
    }

    public void testGetRemoteActionsHappeningBeforeLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");
        Date lateMorning = format.parse("2021-01-01 09:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(lateMorning)
                .position(20)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesEarlier = format.parse("2021-01-01 07:55:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = episodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertSame(0, uniqueList.size());
    }

    public void testGetMultipleRemoteActionsHappeningAfterLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesLater = format.parse("2021-01-01 08:05:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesLater)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = episodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertEquals(2, uniqueList.size());
    }

    public void testGetMultipleRemoteActionsHappeningBeforeLocalActions() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date morning = format.parse("2021-01-01 08:00:00");

        List<EpisodeAction> episodeActions = new ArrayList<>();
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(10)
                .build()
        );
        episodeActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morning)
                .position(5)
                .build()
        );

        Date morningFiveMinutesEarlier = format.parse("2021-01-01 07:55:00");
        List<EpisodeAction> remoteActions = new ArrayList<>();
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.1", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(10)
                .build()
        );
        remoteActions.add(new EpisodeAction
                .Builder("podcast.a", "episode.2", EpisodeAction.Action.PLAY)
                .timestamp(morningFiveMinutesEarlier)
                .position(5)
                .build()
        );

        Map<Pair<String, String>, EpisodeAction> uniqueList = episodeActionFilter
                .getRemoteActionsOverridingLocalActions(remoteActions, episodeActions);
        assertEquals(0, uniqueList.size());
    }
}