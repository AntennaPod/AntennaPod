package com.github.kilianB.sonos.model;

import java.util.Optional;

/**
 * Property of a UPnP Queue Changed Event
 * @author Kilian
 * @see com.github.kilianB.sonos.listener.MediaRendererQueueListener MediaRendererQueueListener
 *
 */
public class QueueEvent {
	
	private int queueId;
	private int updateId;
	private Optional<Boolean> curated;
	
	public QueueEvent(int queueId, int updateId) {
		super();
		this.queueId = queueId;
		this.updateId = updateId;
		this.curated = Optional.empty();
	}
	
	public Optional<Boolean> getCurated() {
		return curated;
	}

	public void setCurated(Optional<Boolean> curated) {
		this.curated = curated;
	}

	public int getQueueId() {
		return queueId;
	}
	public void setQueueId(int queueId) {
		this.queueId = queueId;
	}
	public int getUpdateId() {
		return updateId;
	}
	public void setUpdateId(int updateId) {
		this.updateId = updateId;
	}

	@Override
	public String toString() {
		return "QueueEvent [queueId=" + queueId + ", updateId=" + updateId + ", curated=" + curated + "]";
	}

}
