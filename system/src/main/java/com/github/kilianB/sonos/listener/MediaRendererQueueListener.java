package com.github.kilianB.sonos.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jdom2.Element;
import org.jdom2.Namespace;

import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.model.QueueEvent;
import com.github.kilianB.uPnPClient.UPnPEvent;
import com.github.kilianB.uPnPClient.UPnPEventAdapter;
import com.github.kilianB.uPnPClient.UPnPEventAdapterVerbose;

/**
 * Event listener used to parse UPnPEvents received from the Queue service 
 * relating to queue management, saving queues etc
 * @author Kilian
 *
 */
public class MediaRendererQueueListener extends UPnPEventAdapter {

	private static final Namespace upnpQueueNamespace= Namespace.getNamespace("urn:schemas-sonos-com:metadata-1-0/Queue/");
	
	/**
	 * Event listeners to be notified in case of noteworthy events
	 */
	private final List<SonosEventListener> listeners;
	
	public MediaRendererQueueListener(String servicePath,SonosDevice device) {
		//super(servicePath);
		this.listeners = device.getEventListener();
	}

	@Override
	public void initialEventReceived(UPnPEvent event) {
		//System.out.println("Initial event: ");
		//System.out.println(event.getBodyAsString());
	}

	@Override
	public void eventReceived(UPnPEvent event) {
		//System.out.println("Value changed event: ");
		//System.out.println(event.getBodyAsString());
		
		for(Element e : event.getProperties()) {
			
			List<Element> modifiedQueues = e.getChild("Event",upnpQueueNamespace).getChildren("QueueID",upnpQueueNamespace);
			
			List<QueueEvent> queuesAffected= new ArrayList<QueueEvent>();
			
			for(Element ee : modifiedQueues) {
		
				int queueId = Integer.parseInt(ee.getAttributeValue("val"));
				int updatedId = -1;
				
				Element updateID = ee.getChild("UpdateID",upnpQueueNamespace);
				
				if(updateID != null) {
					updatedId = Integer.parseInt(updateID.getAttributeValue("val"));
				}
				

				QueueEvent queue = new QueueEvent(queueId,updatedId);
				
				
				Element curated = ee.getChild("Curated",upnpQueueNamespace);
				if(curated != null) {
					queue.setCurated(Optional.of(Integer.parseInt(curated.getAttributeValue("val")) != 0));
				}
				
				queuesAffected.add(queue);
			}
			for(SonosEventListener listener : this.listeners) {
				listener.queueChanged(queuesAffected);
			}
		}
		
	}
}
