package com.github.kilianB.sonos.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jdom2.Element;

import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.uPnPClient.UPnPEvent;
import com.github.kilianB.uPnPClient.UPnPEventAdapter;
import com.github.kilianB.uPnPClient.UPnPEventAdapterVerbose;

/**
 * Sample event listener for the sonos topology events. Topology events
 * take care how speakers are grouped together and which devices are present on the 
 * network.
 * 
 * TODO ZoneTopology really should be a singleton or only one device should be subscribed to. 
 * This will need refactoring in the future
 * 
 * @author Kilian
 *
 */
public class ZoneTopologyListener extends UPnPEventAdapter {


	/**
	 * Devices currently available in the network
	 */
	private HashSet<String> presentDevices = new HashSet<String>();
	
	/**
	 * Internal state of the devices current grouping state
	 */
	private HashSet<String> groupState = new HashSet<String>();
	
	/**
	 * Event listeners to be notified in case of noteworthy events
	 */
	private List<SonosEventListener> listeners;

	private SonosDevice device;
	
	public ZoneTopologyListener(String servicePath, SonosDevice device) {
		//super(servicePath);
		this.device = device;
		this.listeners = device.getEventListener();
	}

	@Override
	public void initialEventReceived(UPnPEvent event) {
		presentDevices.addAll(parseConnectedDeviceNames(event));
	}

	@Override
	public void eventReceived(UPnPEvent event) {

		HashSet<String> currentConnectedDevices = parseConnectedDeviceNames(event);

		currentConnectedDevices.stream().forEach(deviceName -> {
			if (!presentDevices.contains(deviceName)) {
				for (SonosEventListener listener : listeners) {
					listener.sonosDeviceConnected(deviceName);
				}
			}
		});

		if (presentDevices.removeAll(currentConnectedDevices)) {
			presentDevices.stream().forEach(deviceName -> {
				for (SonosEventListener listener : listeners) {
					listener.sonosDeviceDisconnected(deviceName);
				}
			});
		}
		presentDevices = currentConnectedDevices;
		
		//We could also emit events when devices get grouped
	}

	private HashSet<String> parseConnectedDeviceNames(UPnPEvent event) {
		String ownName = device.getDeviceNameCached();
		HashSet<String> devices = new HashSet<String>();
		// Are we interested about the household ids?
		for (Element property : event.getProperties()) {

			if (property.getName().equals("ZoneGroupState")) {
				List<Element> zoneGroups = property.getChild("ZoneGroups").getChildren("ZoneGroup");

				
				for (Element zoneGroup : zoneGroups) {
					ArrayList<String> allDevicesInZone = new ArrayList<String>();
					for (Element device : zoneGroup.getChildren("ZoneGroupMember")) {
						String deviceName = device.getAttributeValue("ZoneName");
						allDevicesInZone.add(deviceName);
					}
					if(allDevicesInZone.contains(ownName)) {
						//Check if we have a group change event
						if(!groupState.isEmpty()) {
							
							if( !groupState.containsAll(allDevicesInZone) || groupState.size() != allDevicesInZone.size()) {
								groupState.clear();
								for (SonosEventListener listener : listeners) {
									listener.groupChanged(allDevicesInZone);
								}
							}
						}
						groupState.addAll(allDevicesInZone);
						
					}
					devices.addAll(allDevicesInZone);
				}
			}
		}
		return devices;
	}

}
