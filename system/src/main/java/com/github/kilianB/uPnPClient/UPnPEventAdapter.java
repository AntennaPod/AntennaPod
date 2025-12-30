package com.github.kilianB.uPnPClient;

/**
 * Adapter class of {@link UPnPEventListener} used as callback for 
 * UPnPEvents emitted from subscriptions.
 * @author Kilian
 *
 */
public class UPnPEventAdapter implements UPnPEventListener{

	@Override
	public void initialEventReceived(UPnPEvent event) {}

	@Override
	public void eventReceived(UPnPEvent event) {}

	@Override
	public void eventSubscriptionExpired() {}

	@Override
	public void renewalFailed(Exception e) {}

	@Override
	public void unsubscribed() {}

}
