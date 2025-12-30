package com.github.kilianB.uPnPClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A basic implementation of the UPnP (Universal Plug and Play) Simple Device
 * Discovery Protocol allowing to discover devices via UDP
 * 
 * @see <a href="http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP-arch-DeviceArchitecture-v1.1</a>
 * @author Kilian
 *
 */
public class SimpleDeviceDiscovery {

	public static final String USER_AGENT = "Linux UPnP/1.0 Sonos/42.2-52113 (WDCR:Microsoft Windows NT 10.0.16299)";

	private static final Logger LOGGER = Logger.getLogger(SimpleDeviceDiscovery.class.getName());

	private static final String UPNP_HOST = "239.255.255.250";
	private static final int UPNP_PORT = 1900;

	private static final String DISCOVERY_REQUEST = "M-SEARCH * HTTP/1.1\r\n" + "HOST:  " + UPNP_HOST + ":" + UPNP_PORT
			+ "\r\n"	// SSDP address
			+ "MAN: \"ssdp:discover\"\r\n"				// HTTP extension framework header
			+ "MX: {mx}\r\n"							// Random delay Allowed values 1 - 5
			+ "ST: ssdp:all\r\n\r\n";					// Search type see documentation
	// + Optional user agent

	/**
	 * Discover all devices advertising themselves via the Simple Device Discovery
	 * Protocol
	 * @return discovered devices
	 * @throws IOException if an I/O error occurs.
	 */
	public static List<UPnPDevice> discoverDevices() throws IOException {
		return discoverDevices(1, 2, null);
	}

	/**
	 * Discover all devices advertising themselves via the Simple Device Discovery
	 * Protocol matching the given search target
	 * 
	 * @param searchTarget
	 *            The search target to select specific devices or services
	 * @return discovered devices
	 * @throws IOException if an I/O error occurs.
	 */
	public static List<UPnPDevice> discoverDevices(String searchTarget) throws IOException {
		return discoverDevices(1, 2, searchTarget);
	}

	/**
	 * 
	 * @param loadBalancingDelay
	 *            specifies a range, devices may delay their response to lessen
	 *            load. may be in the range of [1-5]s
	 * @param timeout
	 *            The number of seconds waited before the search is aborted
	 * @param searchTarget
	 *            The search target to select specific devices or services
	 * @param callback event handler called once a device was found
	 * @return discovered devices
	 * @throws IOException	 if an I/O error occurs.
	 */
	public static List<UPnPDevice> discoverDevices(int loadBalancingDelay, int timeout, String searchTarget,UPnPDeviceFoundListener callback)
			throws IOException {

		ArrayList<UPnPDevice> devicesFound = new ArrayList<UPnPDevice>();

		timeout *= 1000;
		String request = prepareRequest(loadBalancingDelay, timeout, searchTarget);
		// Create a udp package

		byte[] payload = request.getBytes();
		DatagramPacket discoveryRequest = new DatagramPacket(payload, payload.length,
				new InetSocketAddress(UPNP_HOST, UPNP_PORT));

		DatagramSocket udpSocket = new DatagramSocket();
		// devices
		udpSocket.setSoTimeout(timeout * 1000);
		udpSocket.send(discoveryRequest);

		// Work with response
		long startTime = System.currentTimeMillis();

		// Reuse old package

		while (true) {
			byte[] response = new byte[1024];
			DatagramPacket incommingPacket = new DatagramPacket(response, response.length);
			try {
				udpSocket.receive(incommingPacket);
			} catch (SocketTimeoutException timeouted) {
				break;
			}

			HashMap<String, String> deviceInfo = parseUpnpNotifyAndSearchMessage(new String(incommingPacket.getData()));

			UPnPDevice device = new UPnPDevice(incommingPacket.getAddress(), deviceInfo);
			devicesFound.add(device);
			
			if(callback != null) {
				new Thread( ()->{
					callback.upnpDeviceFound(device);
				}).start();
			}
			
			/**
			 * if there is an error with the search request (such as an invalid field value
			 * in the MAN header field, a missing MX header field, or other malformed
			 * content), the device MUST silently discard and ignore the search request;
			 * sending of error responses is PROHIBITED due to the possibility of packet
			 * storms if many devices send an error response to the same request.
			 */

			// time left until timeout
			int timeLeft = timeout - (int) (System.currentTimeMillis() - startTime);
			if (timeLeft <= 0) {
				break;
			}
			udpSocket.setSoTimeout(timeLeft);
		}

		udpSocket.close();

		return devicesFound;
	}
	
	/**
	 * 
	 * @param loadBalancingDelay
	 *            specifies a range, devices may delay their response to lessen
	 *            load. may be in the range of [1-5]s
	 * @param timeout
	 *            The number of seconds waited before the search is aborted
	 * @param searchTarget
	 *            The search target to select specific devices or services
	 * @return discovered devices
	 * @throws IOException	 if an I/O error occurs.
	 */
	public static List<UPnPDevice> discoverDevices(int loadBalancingDelay, int timeout, String searchTarget)
			throws IOException {
		return discoverDevices(loadBalancingDelay,timeout,searchTarget,null);
	}


	
	/**
	 * Return the first device discovered matching the supplied search target. This
	 * method will wait a maximum of 2 seconds before aborting the search.
	 * 
	 * @param searchTarget
	 *            The search target to select specific devices or services
	 * @throws IOException  if an I/O error occurs.
	 * @return the UPnPDevice found or null if no device was found
	 */
	public static UPnPDevice discoverDevice(String searchTarget) throws IOException {
		return discoverDevice(1, 2, searchTarget);
	}

	/**
	 * Return the first device discovered matching the supplied search target.
	 * 
	 * @param timeout
	 *            The number of seconds waited before the search is aborted
	 * @param searchTarget
	 *            The search target to select specific devices or services
	 * @throws IOException  if an I/O error occurs.
	 * @return the UPnPDevice found or null if no device was found
	 */
	public UPnPDevice discoverDevice(int timeout, String searchTarget) throws IOException {
		return discoverDevice(1, timeout, searchTarget);
	}

	/**
	 * Return the first device discovered matching the supplied search target.
	 * 
	 * @param loadBalancingDelay
	 *            specifies a range, devices may delay their response to lessen
	 *            load. may be in the range of [1-5]s
	 * @param timeout
	 *            The number of seconds waited before the search is aborted
	 * @param searchTarget
	 *            The search target to select specific devices or services
	 * @throws IOException  if an I/O error occurs.
	 * @return the UPnPDevice found or null if no device was found
	 */
	public static UPnPDevice discoverDevice(int loadBalancingDelay, int timeout, String searchTarget)
			throws IOException {
		timeout *= 1000;
		String request = prepareRequest(loadBalancingDelay, timeout, searchTarget);

		byte[] payload = request.getBytes();
		DatagramPacket discoveryRequest = new DatagramPacket(payload, payload.length,
				new InetSocketAddress(UPNP_HOST, UPNP_PORT));

		DatagramSocket udpSocket = new DatagramSocket();
		// devices
		udpSocket.setSoTimeout(timeout * 1000);
		udpSocket.send(discoveryRequest);

		byte[] returnValue = new byte[1024];
		try {
			DatagramPacket incommingPacket = new DatagramPacket(returnValue, returnValue.length);
			udpSocket.receive(incommingPacket);
			udpSocket.close();
			return new UPnPDevice(incommingPacket.getAddress(),
					parseUpnpNotifyAndSearchMessage(new String(incommingPacket.getData())));
		} catch (SocketTimeoutException timeouted) {
			udpSocket.close();
			return null;
		}

	}

	/* Utility functions*/

	/**
	 * 
	 * @param loadBalancingDelay
	 *            in seconds
	 * @param timeout
	 *            in miliseconds
	 * @param searchTarget
	 * @return	upnp request as string
	 */
	private static String prepareRequest(int loadBalancingDelay, int timeout, String searchTarget) {
		if (loadBalancingDelay < 1 || loadBalancingDelay > 5) {
			LOGGER.warning("Load balancing delay should be within [1-5] seconds. A default of 1s is assumed");
			loadBalancingDelay = 1;
		}

		if (loadBalancingDelay >= (timeout / 1000)) {
			LOGGER.warning(
					"Load balancing delay should not be higher or equal than the timeout. This will lead to some devices not being discovered!");
		}

		String request = DISCOVERY_REQUEST;

		if (searchTarget != null && !searchTarget.isEmpty() && !searchTarget.equals("ssdp:all")) {
			request = request.replace("ssdp:all", searchTarget);
		}
		return request.replace("{mx}", Integer.toString(loadBalancingDelay));
	}

	private static HashMap<String, String> parseUpnpNotifyAndSearchMessage(String messageToParse) {
		final Matcher matcher = Pattern.compile("(.*?):(.*)").matcher(messageToParse);

		final HashMap<String, String> parsedKeyValues = new HashMap<String, String>();

		while (matcher.find()) {
			parsedKeyValues.put(matcher.group(1), matcher.group(2));
		}
		return parsedKeyValues;
	}
	
	public interface UPnPDeviceFoundListener{
		
		void upnpDeviceFound(UPnPDevice device);
		
	}

}
