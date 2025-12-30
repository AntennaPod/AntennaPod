package com.github.kilianB.uPnPClient;

/**
 * Event listener used to listen to UPnP Event emitted by subscriptions 
 * of services.
 * 
 * <code>
 * 	UPnPDevice.subscribe(UPnPEventListener,ServicePath);
 * </code>
 *  
 * @author Kilian
 * @see UPnPEventAdapter UPnPEventAdapter for an adapter version
 * @see UPnPEventAdapterVerbose UPnPEventAdapterVerbose as a default event listener implementation
 *
 */
public interface UPnPEventListener {
	
	/**
	 * Notifies about the arrival of the first UPnPEvent made after subscription to the service.
	 * The first event usually contains information about the current state of the service/device.
	 * @param event The event send by the server
	 */
	public void initialEventReceived(UPnPEvent event);
	
	
	/**
	 * Notifies about the arrival of all but the very first event.
	 * @param event The event send by the server
	 */
	public void eventReceived(UPnPEvent event);
	
	/**
	 * Called once the event subscription period expired and no renewal was issued
	 */
	public void eventSubscriptionExpired();


	/**
	 * Method will be called if the re-subscription to the event failed.
	 * From this point on no events will be received..
	 * @param e	Exception why the renewal failed
	 */
	public void renewalFailed(Exception e);
	
	/**
	 * Notifies about a successful unsubscription of the subscribed event.
	 */
	public void unsubscribed();	
}
