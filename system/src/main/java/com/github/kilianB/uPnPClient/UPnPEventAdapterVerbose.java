package com.github.kilianB.uPnPClient;

import org.jdom2.Element;

import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * A default {@link UPnPEventListener} implementation logging the content of events 
 * allowing the first inspection of the received content.
 * 
 * @author Kilian
 *
 */
public class UPnPEventAdapterVerbose implements UPnPEventListener{

	private final static Logger LOGGER = Logger.getLogger(UPnPEventAdapterVerbose.class.getName());
	
	private final String servicePath;
	
	public UPnPEventAdapterVerbose(String servicePath) {
		this.servicePath = servicePath;
	}
	
	@Override
	public void initialEventReceived(UPnPEvent event) {
		LOGGER.fine("inital event");
		LOGGER.info(event.toString());
		LOGGER.info(event.getBodyAsString());
	}

	@Override
	public void eventReceived(UPnPEvent event) {
		LOGGER.fine("value changed event");
		LOGGER.info(event.toString());
		LOGGER.info(event.getBodyAsString());
		for(Element e : event.getProperties()) {
			LOGGER.info(e.toString());
		}
	}

	@Override
	public void eventSubscriptionExpired() {
		LOGGER.severe(MessageFormat.format("Event subscription for: {0} expired", servicePath));
	}

	@Override
	public void renewalFailed(Exception e) {
		LOGGER.severe(MessageFormat.format("Renewal subscroption for: {0} failed", servicePath));
	}

	@Override
	public void unsubscribed() {
		LOGGER.info(MessageFormat.format("Unsubscribed from {0}", servicePath));
	}
	
}