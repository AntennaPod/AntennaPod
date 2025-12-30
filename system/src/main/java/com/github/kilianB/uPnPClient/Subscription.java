package com.github.kilianB.uPnPClient;

import java.util.concurrent.ScheduledFuture;

/**
 * Represents a successfully established subscription to a service of an UPnP device
 * @author Kilian
 *
 */
public class Subscription {

	/**
	 * Serivce identifier of the event
	 */
	private String token;
	/**
	 * Path of the service used to subscribe to 
	 */
	private String servicePath;
	/**
	 * Listener which will be notified in case an event takes place
	 */
	private UPnPEventListener eventListener;
	/**
	 * Future handling re subscription 
	 */
	private ScheduledFuture<?> renewalFuture;
	/**
	 * Interval in seconds between re subscriptions
	 */
	private int renewalInterval;
	/**
	 * Current sequence identifier of the subscription.
	 */
	private int sequenceCount = -1;

	public Subscription(UPnPEventListener eventListener, String servicePath, int renewalInterval) {
		this.eventListener = eventListener;
		this.servicePath = servicePath;
		this.renewalInterval = renewalInterval;
	}

	/**
	 * @return service identifier of the event subscription
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token service identifier of the event subscription emitted by the device
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @return the relative path used to subscribe to the service 
	 */
	public String getServicePath() {
		return servicePath;
	}

	/**
	 * @param servicePath the relative path of the service
	 */
	public void setServicePath(String servicePath) {
		this.servicePath = servicePath;
	}

	/**
	 * The renewal future is the currently scheduled future to issue a renewal request for the described
	 * event subscription
	 * @return the renewal future used to cancel the renewal request
	 */
	public ScheduledFuture<?> getRenewalFuture() {
		return renewalFuture;
	}

	/**
	 * Sets the subscriptions renewal future used to renew the event subscription
	 * @param renewalFuture	the future in charge of resubscribing
	 */
	public void setRenewalFuture(ScheduledFuture<?> renewalFuture) {
		this.renewalFuture = renewalFuture;
	}

	/**
	 * The current sequence count of the subscription event. The sequence count increases
	 * with every received event and can be used to track the sequential order as well as if
	 * events were lost due to UDP unreliability
	 * @return the last submitted sequence count submitted by the device
	 */
	public int getSequenceCount() {
		return sequenceCount;
	}

	public void setSequenceCount(int sequenceCount) {
		this.sequenceCount = sequenceCount;
	}

	public UPnPEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(UPnPEventListener eventListener) {
		this.eventListener = eventListener;
	}

	public int getRenewalInterval() {
		return renewalInterval;
	}

	public void setRenewalInterval(int renewalInterval) {
		this.renewalInterval = renewalInterval;
	}

}
