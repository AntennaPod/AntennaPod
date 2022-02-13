package de.danoeh.antennapod.playback.base;

import java.util.List;
import java.util.Random;

public class Shuffle {
    public int shuffle(List<Integer> queue) {
        Random rand = new Random();
        int upperbound = queue.size();

        return rand.nextInt(upperbound);
    }
}
