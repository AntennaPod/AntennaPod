package de.danoeh.antennapod.ui.screen.queue;

import de.danoeh.antennapod.model.queue.Queue;

public class QueueInfo {
    private final Queue queue;
    private final int itemCount;
    private final long timeLeft;

    public QueueInfo(Queue queue, int itemCount, long timeLeft) {
        this.queue = queue;
        this.itemCount = itemCount;
        this.timeLeft = timeLeft;
    }

    public Queue getQueue() {
        return queue;
    }

    public int getItemCount() {
        return itemCount;
    }

    public long getTimeLeft() {
        return timeLeft;
    }
}
