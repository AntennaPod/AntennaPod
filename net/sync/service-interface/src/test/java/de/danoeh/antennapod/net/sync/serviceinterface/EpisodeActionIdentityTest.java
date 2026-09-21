package de.danoeh.antennapod.net.sync.serviceinterface;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class EpisodeActionIdentityTest {
    private static EpisodeAction.Builder builder() {
        return builder("podcast", "episode", EpisodeAction.PLAY);
    }

    private static EpisodeAction.Builder builder(String podcast, String episode, EpisodeAction.Action kind) {
        return new EpisodeAction.Builder(podcast, episode, kind)
                .timestamp(new Date(1609488000000L))
                .guid("guid")
                .started(1)
                .position(2)
                .total(3);
    }

    @Test
    public void actionsBuiltFromTheSameValuesAreEqualWithTheSameHashCode() {
        EpisodeAction first = builder().build();
        EpisodeAction second = builder().build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void actionsWithoutAnyOptionalValuesAreEqualWithTheSameHashCode() {
        EpisodeAction first = new EpisodeAction.Builder(null, null, null).build();
        EpisodeAction second = new EpisodeAction.Builder(null, null, null).build();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void actionsDifferingInAnyValueAreNotEqual() {
        EpisodeAction action = builder().build();

        assertNotEquals(action, builder("other", "episode", EpisodeAction.PLAY).build());
        assertNotEquals(action, builder("podcast", "other", EpisodeAction.PLAY).build());
        assertNotEquals(action, builder("podcast", "episode", EpisodeAction.DELETE).build());
        assertNotEquals(action, builder().guid("other").build());
        assertNotEquals(action, builder().timestamp(new Date(1609488000001L)).build());
        assertNotEquals(action, builder().started(4).build());
        assertNotEquals(action, builder().position(5).build());
        assertNotEquals(action, builder().total(6).build());
    }

    @Test
    public void actionIsEqualToItself() {
        EpisodeAction action = builder().build();

        assertTrue(action.equals(action));
    }

    @Test
    public void actionIsNotEqualToNullOrOtherTypes() {
        EpisodeAction action = builder().build();

        assertFalse(action.equals(null));
        assertFalse(action.equals("podcast"));
    }

    @Test
    public void descriptionContainsAllIdentifyingValues() {
        String description = builder().build().toString();

        assertTrue(description.contains("podcast='podcast'"));
        assertTrue(description.contains("episode='episode'"));
        assertTrue(description.contains("guid='guid'"));
        assertTrue(description.contains("action=PLAY"));
        assertTrue(description.contains("started=1"));
        assertTrue(description.contains("position=2"));
        assertTrue(description.contains("total=3"));
    }
}
