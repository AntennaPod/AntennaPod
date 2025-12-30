package com.github.kilianB.sonos;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.uPnPClient.SimpleDeviceDiscovery;
import com.github.kilianB.uPnPClient.UPnPDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple Device Discovery Protocol (SSDP) for sons speakers
 * 
 * @author Kilian
 * @author vmichalak
 */
public class SonosDiscovery {

	/**
	 * Scan duration in seconds
	 */
	private static final int DEFAULT_SCAN_DURATION = 2;
	private static final String SONOS_URN = "urn:schemas-upnp-org:device:ZonePlayer:1";

	// Hide the implicit public constructor.
	private SonosDiscovery() {
	}

	/**
	 * Discover all SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol).
	 * 
	 * @return List of SONOS speakers
	 * @throws IOException network exception during
	 */
	public static List<SonosDevice> discover() throws IOException {
		return discover(DEFAULT_SCAN_DURATION);
	}

	/**
	 * Discover all SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol).
	 * 
	 * @param scanDuration The number of seconds to wait while scanning for devices.
	 * @return List of SONOS speakers
	 * @throws IOException network exception during device discovery
	 */
	public static List<SonosDevice> discover(int scanDuration) throws IOException {
		List<UPnPDevice> source = SimpleDeviceDiscovery.discoverDevices(1, scanDuration, SONOS_URN);
		ArrayList<SonosDevice> output = new ArrayList<SonosDevice>();
		for (UPnPDevice device : source) {
			output.add(new SonosDevice(device));
		}
		return Collections.unmodifiableList(output);
	}

	/**
	 * Discover all SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol) in an asynch manner. This method does not throw an IO Error!.
	 * 
	 * @param scanDuration The number of seconds to wait while scanning for devices.
	 * @param callback	listener to be notified about found devices
	 */
	public static void discoverAsynch(int scanDuration, SonosDeviceFoundListener callback){
		new Thread(()-> {
			ArrayList<SonosDevice> output = new ArrayList<SonosDevice>();
			try {
				SimpleDeviceDiscovery.discoverDevices(1, scanDuration, SONOS_URN, (upnpDevice) -> {
					SonosDevice sonosDevice = new SonosDevice(upnpDevice);
					output.add(sonosDevice);
					callback.deviceFound(sonosDevice);
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Discover one SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol).
	 * 
	 * @return SONOS speaker
	 * @throws IOException network exception during device discovery
	 */
	public static SonosDevice discoverOne() throws IOException {
		return discoverOne(DEFAULT_SCAN_DURATION);
	}

	/**
	 * Discover one SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol).
	 * 
	 * @param scanDuration The number of seconds to wait while scanning for devices.
	 * @return SONOS speaker
	 * @throws IOException network exception during device discovery
	 */
	public static SonosDevice discoverOne(int scanDuration) throws IOException {
		UPnPDevice source = new SimpleDeviceDiscovery().discoverDevice(scanDuration, SONOS_URN);
		if (source == null) {
			return null;
		}
		return new SonosDevice(source);
	}

	/**
	 * Discover one SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol) by UID.
	 * 
	 * @param uid Sonos Speaker UID
	 * @return SONOS speaker
	 * @throws IOException network exception during device discovery
	 */
	public static SonosDevice discoverByUID(String uid) throws IOException {
		return discoverByUID(uid, DEFAULT_SCAN_DURATION);
	}

	/**
	 * Discover one SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol) by UID.
	 * 
	 * @param uid          Sonos Speaker UID
	 * @param scanDuration The number of seconds to wait while scanning for devices.
	 * @return SONOS speaker
	 * @throws IOException network exception during device discovery
	 */
	public static SonosDevice discoverByUID(String uid, int scanDuration) throws IOException {
		UPnPDevice source = new SimpleDeviceDiscovery().discoverDevice(scanDuration, "uuid:" + uid);
		if (source == null) {
			return null;
		}
		return new SonosDevice(source);
	}

	/**
	 * Discover one SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol) by name.
	 * 
	 * @param name Sonos Speaker name.
	 * @return Sonos speaker (or null if no speaker was found)
	 * @throws IOException network exception during device discovery
	 */
	public static SonosDevice discoverByName(String name) throws IOException {
		return discoverByName(name, DEFAULT_SCAN_DURATION);
	}

	/**
	 * Discover one SONOS speakers on network using SSDP (Simple Service Discovery
	 * Protocol) by name.
	 * 
	 * @param name         Sonos Speaker name.
	 * @param scanDuration The number of milliseconds to wait while scanning for
	 *                     devices.
	 * @return Sonos speaker (or null if no speaker was found)
	 * @throws IOException network exception during device discovery
	 */
	public static SonosDevice discoverByName(String name, int scanDuration) throws IOException {
		List<SonosDevice> sonosDevices = SonosDiscovery.discover(scanDuration);
		for (SonosDevice sonosDevice : sonosDevices) {
			try {
				if (sonosDevice.getZoneName().equalsIgnoreCase(name)) {
					return sonosDevice;
				}
			} catch (SonosControllerException e) {
				/* ignored */ }
		}
		return null;
	}

	@FunctionalInterface
	public interface SonosDeviceFoundListener {
		void deviceFound(SonosDevice device);
	}
}