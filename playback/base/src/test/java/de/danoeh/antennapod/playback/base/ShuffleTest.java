package de.danoeh.antennapod.playback.base;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ShuffleTest {

    @Test
    public void givenList_whenRandomIndexChosen_shouldReturnRandomIndex() {
        // init
        List<Integer> queue = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Shuffle shuffle = new Shuffle();

        // shuffle
        int randomIndex = shuffle.shuffle(queue);

        // assert
        assertTrue(0 <= randomIndex && randomIndex <= queue.size()-1);
    }
}
