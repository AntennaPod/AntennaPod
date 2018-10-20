package de.danoeh.antennapod.core.feed;

import android.os.Handler;
import android.util.Log;

import java.util.AbstractQueue;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Notifies its observers about changes in the feed database. Observers can
 * register by retrieving an instance of this class and registering an
 * EventListener. When new events arrive, the EventDistributor will process the
 * event queue in a handler that runs on the main thread. The observers will only
 * be notified once if the event queue contains multiple elements.
 * 
 * Events can be sent with the send* methods.
 */
public class EventDistributor extends Observable {
	private static final String TAG = "EventDistributor";

	public static final int FEED_LIST_UPDATE = 1;
	public static final int UNREAD_ITEMS_UPDATE = 2;
	public static final int DOWNLOADLOG_UPDATE = 8;
	public static final int PLAYBACK_HISTORY_UPDATE = 16;
	public static final int DOWNLOAD_HANDLED = 64;
    public static final int PLAYER_STATUS_UPDATE = 128;

	private final Handler handler;
	private final AbstractQueue<Integer> events;

	private static EventDistributor instance;

	private EventDistributor() {
		this.handler = new Handler();
		events = new ConcurrentLinkedQueue<>();
	}

	public static synchronized EventDistributor getInstance() {
		if (instance == null) {
			instance = new EventDistributor();
		}
		return instance;
	}

	public void register(EventListener el) {
		addObserver(el);
	}

	public void unregister(EventListener el) {
		deleteObserver(el);
	}

	private void addEvent(Integer i) {
		events.offer(i);
		handler.post(EventDistributor.this::processEventQueue);
	}

	private void processEventQueue() {
		Integer result = 0;
		Log.d(TAG, "Processing event queue. Number of events: " + events.size());
		for (Integer current = events.poll(); current != null; current = events
				.poll()) {
			result |= current;
		}
		if (result != 0) {
			Log.d(TAG, "Notifying observers. Data: " + result);
			setChanged();
			notifyObservers(result);
		} else {
			Log.d(TAG, "Event queue didn't contain any new events. Observers will not be notified.");
		}
	}

	@Override
	public void addObserver(Observer observer) {
		super.addObserver(observer);
	}

	public void sendUnreadItemsUpdateBroadcast() {
		addEvent(UNREAD_ITEMS_UPDATE);
	}

	public void sendFeedUpdateBroadcast() {
		addEvent(FEED_LIST_UPDATE);
	}

	public void sendPlaybackHistoryUpdateBroadcast() {
		addEvent(PLAYBACK_HISTORY_UPDATE);
	}

	public void sendDownloadLogUpdateBroadcast() {
		addEvent(DOWNLOADLOG_UPDATE);
	}

    public void sendPlayerStatusUpdateBroadcast() { addEvent(PLAYER_STATUS_UPDATE); }

	public abstract static class EventListener implements Observer {

		@Override
		public void update(Observable observable, Object data) {
			if (observable instanceof EventDistributor
					&& data instanceof Integer) {
				update((EventDistributor) observable, (Integer) data);
			}
		}

		public abstract void update(EventDistributor eventDistributor,
				Integer arg);
	}
}
