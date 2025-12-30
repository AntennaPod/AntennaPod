package com.github.kilianB.uPnPClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.github.kilianB.DaemonThread;
import com.github.kilianB.DaemonThreadFactory;
import com.github.kilianB.NetworkUtil;
import com.github.kilianB.StringUtil;
import com.github.kilianB.sonos.ParserHelper;

/**
 * A UPnPDevice represents a single physical device. The class is used to
 * interact with UPnP subscriptions.
 * 
 * @author Kilian
 *
 */
public class UPnPDevice {

	// UPnP Event Errorcode
	private static final int INCOMPATIBLE_HEADER_FIELDS = 400;
	private static final int PRECONDITION_FAILED = 412; // Token invalid or missing

	private static final String ACKNOWLEDGEMENT = "HTTP/1.1 200 OK\r\n" + "Server: " + SimpleDeviceDiscovery.USER_AGENT
			+ "\r\n" + "Connection: close\r\n\r\n";

	private static final String BAD_REQUEST = "HTTP/1.1 400 Bad Request\r\n" + "Server: "
			+ SimpleDeviceDiscovery.USER_AGENT + "\r\n" + "Connection: close\r\n\r\n";

	private static final byte[] ACKNOWLEDGEMENT_MESSAGE = ACKNOWLEDGEMENT.getBytes();
	private static final byte[] BAD_REQUEST_MESSAGE = BAD_REQUEST.getBytes();

	private static final Logger LOGGER = Logger.getLogger(UPnPDevice.class.getName());

	private InetAddress deviceAddress;
	/**
	 * The device info contains all information received during device disocery
	 */
	private HashMap<String, String> deviceInfo;

	/**
	 * Lazily initialized event callback socket for upnp events. Reuse this socket
	 * for all events.
	 */
	private ServerSocket eventCallbackSocket;

	/**
	 * Lookup map holding all currently subscribed to event subscriptions. They are
	 * used to gracefully shut down the subscriptions upon jvm exit or during
	 * unsubscription of individual subscriptions.
	 * <p>
	 * Sid -> subscription
	 */
	private ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<String, Subscription>();

	/**
	 * Handle event re-subscriptions
	 */
	private ScheduledExecutorService scheduler;

	/**
	 * Creates a UPnP Device based on the supplied inetAddress.
	 * 
	 * @param inetAddress The address of the UPnP device
	 * @param deviceInfo  Information retrieved durin Simple Device Discovery
	 */
	public UPnPDevice(InetAddress inetAddress, HashMap<String, String> deviceInfo) {
		this.deviceAddress = inetAddress;
		this.deviceInfo = deviceInfo;

		// Register shutdown hook to gracefully close socket connections and unsubscribe
		// from UPnP Events
		Runtime.getRuntime().addShutdownHook(handleShutdown);

	}

	/**
	 * Get the InetAddress of the UPnPDevice
	 * 
	 * @return the address of the device
	 */
	public InetAddress getIP() {
		return deviceAddress;
	}

	/**
	 * get the location information of the device supplied during Simple Device
	 * Discovery step
	 * 
	 * @return Field value contains a URL to the UPnP description of the root
	 *         device. Normally the host portion contains a literal IP address
	 *         rather than a domain name in unmanaged networks
	 * @see <a href=
	 *      "http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP-arch-DeviceArchitecture-v1.1</a>
	 */
	public String getLocation() {
		return deviceInfo.get("LOCATION");
	}

	/**
	 * Get the server property of this UPnP Device. Specified by UPnP vendor.
	 * String. Field value MUST begin with the following �product tokens� (defined
	 * by HTTP/1.1). The first product token identifies the operating system in the
	 * form OS name/OS version, the second token represents the UPnP version and
	 * MUST be UPnP/1.1, and the third token identifies the product using the form
	 * product name/product version. For example, �SERVER: unix/5.1 UPnP/1.1
	 * MyProduct/1.0�. Control points MUST be prepared to accept a higher minor
	 * version number of the UPnP version than the control point itself implements.
	 * For example, control points implementing UDA version 1.0 will be able to
	 * interoperate with devices implementing UDA version 1.1.
	 * 
	 * @see <a href=
	 *      "http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP-arch-DeviceArchitecture-v1.1</a>
	 * @return The server property
	 */
	public String getServer() {
		return deviceInfo.get("SERVER");
	}

	/**
	 * Get the ST property of this UPnP Device. Field value contains Search Target.
	 * Single URI. The response sent by the device depends on the field value of the
	 * ST header field that was sent in the request. In some cases, the device MUST
	 * send multiple response messages as follows.
	 * 
	 * @see <a href=
	 *      "http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP-arch-DeviceArchitecture-v1.1</a>
	 * @return The st field value
	 */
	public String getSearchTarget() {
		return deviceInfo.get("ST");
	}

	/**
	 * Get the Unique Service Name property of this UPnP Device.
	 * 
	 * @see <a href=
	 *      "http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1.pdf">UPnP-arch-DeviceArchitecture-v1.1</a>
	 * @return The usn field value
	 */
	public String getUniqueServiceName() {
		return deviceInfo.get("USN");
	}

	/**
	 * Return the content of a field retrieved during SimpleDeviceDiscovery
	 * 
	 * @param fieldID The id of the header field
	 * @return the content of the field
	 */
	public String getField(String fieldID) {
		return deviceInfo.get(fieldID);
	}

	/**
	 * @return an unmodifiable map of the device info key value pairs received
	 *         during discovery
	 */
	public Map<String, String> getFields() {
		return Collections.unmodifiableMap(deviceInfo);
	}

	/**
	 * Subscribe to a upnp event with a default resubscription interval of 1 hour
	 * 
	 * @param eventHandler The event handler being called once the device sends an
	 *                     event
	 * @param servicePath  The service path of the event
	 * @return the service identifier used to uniquely identify the subscription
	 * @throws IOException IOException thrown during subscription
	 */
	public String subscribe(UPnPEventListener eventHandler, String servicePath) throws IOException {
		return subscribe(eventHandler, servicePath, 3600);
	}

	// Subscribe to events

	/**
	 * Subscribe to an UPnP Event
	 * 
	 * @param eventHandler  The event handler being called once the device sends an
	 *                      event
	 * @param servicePath   The service path of the event
	 * @param renewalPeriod Renewal period in seconds. If {@literal >} 0 a renewal
	 *                      request will be send before the subscription expires.
	 *                      Usual periods are around 1 hour. Has to be {@literal >}
	 *                      60 secs Else the subscription will timeout.
	 * @return the service identifier used to uniquely identify the subscription or
	 *         null if invalid arguments were supplied
	 * @throws IOException Exception thrown during subscription
	 */
	public String subscribe(UPnPEventListener eventHandler, String servicePath, int renewalPeriod) throws IOException {

		Subscription subscription = new Subscription(eventHandler, servicePath, renewalPeriod);

		LOGGER.fine(MessageFormat.format("Subscribe to {0}", servicePath));

		/*
		 * SUBSCRIBE publisher path HTTP/1.1 HOST: publisher host:publisher port
		 * USER-AGENT: OS/version UPnP/1.1 product/version CALLBACK: <delivery URL> NT:
		 * * upnp:event
		 */

		// Create a sever socket and listen to incoming connections if it's not already
		// present
		initSubscription();

		String callbackAddress = "http://" + eventCallbackSocket.getInetAddress().getHostAddress() + ":"
				+ eventCallbackSocket.getLocalPort();

		/* Create the search request */
		StringBuilder eventSubscription = new StringBuilder("SUBSCRIBE ").append(servicePath).append(" HTTP/1.1\r\n")
				.append("HOST: ").append(deviceAddress.getHostAddress() + ":1400").append("\r\n").append("USER-AGENT: ")
				.append(SimpleDeviceDiscovery.USER_AGENT).append("\r\n") // TODO
				// 1.1
				.append("CALLBACK: <").append(callbackAddress).append(">\r\n").append("NT: upnp:event\r\n")
				.append("TIMEOUT: Second-").append(renewalPeriod).append("\r\n\r\n");

		// Send the subscription event to the device

		try (Socket socket = new Socket(deviceAddress, 1400)) {

			InputStream is = socket.getInputStream();

			// Send message
			OutputStream os = socket.getOutputStream();
			os.write(eventSubscription.toString().getBytes());
			os.flush();

			// 30 the device has to respond within 30 secs according to specs
			socket.setSoTimeout(30000);

			// Wait for the subscription identifier
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String response = NetworkUtil.dumpReader(br);
			String token = ParserHelper.findOne("SID: (.*)", response);
			String timeout = ParserHelper.findOne("TIMEOOT:(.*)", response);
			LOGGER.fine(MessageFormat.format("Token: {0}", token));
			LOGGER.fine("Actual timeout: " + timeout);

			subscription.setToken(token);
			subscriptions.put(token, subscription);

			if (renewalPeriod > 0) {

				// The renewal period has to be shorter than the timeout.
				// Renew 1 minutes before the subscription expires
				// Device has to respond within 30 seconds according to specification

				int timeoutPeriod = renewalPeriod - 60;

				LOGGER.fine("Schedule renewal interval" + timeoutPeriod);

				if (timeoutPeriod < 0) {
					LOGGER.severe(
							"Invalid renewal period specified. UPnP Subscription timeout has to be in the range of (60,]");
					return null;
				}

				// Do some quick checks for sane input values
				if (timeoutPeriod < 60) {
					LOGGER.warning("Short renewal periods are discouraged.");
				}

				// TODO what happens if we have an error in this thread? Will the scheduler
				// still work?
				ScheduledFuture<?> eventResubscription = scheduler.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						try {
							renewSubscription(subscription);
						} catch (IOException e) {
							e.printStackTrace();
							// Cancle itself
							subscription.getRenewalFuture().cancel(false);
							eventHandler.renewalFailed(e);
							subscriptions.remove(subscription.getToken());
						}
					}
				}, timeoutPeriod, timeoutPeriod, TimeUnit.SECONDS);

				subscription.setRenewalFuture(eventResubscription);
			}
			return token;
		}
		// TODO timeout in case subscription failed

	}

	/**
	 * Initialize the server socket to be able to receive UPNP Event callbacks. Only
	 * need to be called once
	 * 
	 * @throws IOException during socket creation.
	 */
	private void initSubscription() throws IOException {
		if (eventCallbackSocket == null) {
			InetAddress host = NetworkUtil.resolveSiteLocalAddress();
			eventCallbackSocket = new ServerSocket(0, 50, host);
			// Start listening to events
			uPnPEventSocketListener.start();
			scheduler = Executors.newScheduledThreadPool(1);

		}
	}

	/**
	 * Issue a renewal request
	 * 
	 * @param subscription Subscription which will be renewed
	 * @throws IOException IOException thrown when device can not be reached
	 */
	private void renewSubscription(Subscription subscription) throws IOException {

		System.out.println("Renew subscription");
		/* Create the search request */
		StringBuilder eventRenewalMessage = new StringBuilder("SUBSCRIBE ").append(subscription.getServicePath())
				.append(" HTTP/1.1\r\n").append("HOST: ").append(deviceAddress.getHostAddress() + ":1400")
				.append("\r\n") // 1.1
				.append("SID: ").append(subscription.getToken()).append("r\n")
				.append("TIMEOUT: Second-" + subscription.getRenewalInterval()).append("\r\n\r\n");

		try (Socket socket = new Socket(deviceAddress, 1400)) {
			InputStream is = socket.getInputStream();

			// Send message
			OutputStream os = socket.getOutputStream();
			os.write(eventRenewalMessage.toString().getBytes());
			os.flush();

			// Wait for the subscription identifier
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String response = NetworkUtil.dumpReader(br);

			if (response.contains("200 OK")) {
				String resend = ParserHelper.findOne("SID: (.*)", response);
				System.out.println("Resubscription: " + response);
				LOGGER.fine(MessageFormat.format("Token: {0}", resend));
				System.out.println("Token:" + resend);
			} else if (response.contains("412 Precondition Failed")) {

				UPnPEventListener eventHandler = subscription.getEventListener();
				eventHandler.renewalFailed(new Exception("412 Precondition Failed"));
				subscription.getRenewalFuture().cancel(true);
				// TODO Does it directly expire if we fail to resubscribe?
				eventHandler.eventSubscriptionExpired();
			} else {
				// TODO 400 incompatible header fields
				// TODO 5xx unable to accept. device internal error
				LOGGER.severe("Unspecified error during renew subscription");
			}

		}
	}

	public boolean unsubscribeFromToken(String sid) {
		if (subscriptions.containsKey(sid)) {
			return unsubscribe(subscriptions.get(sid));
		} else {
			LOGGER.warning(MessageFormat.format(
					"Could not unsubscribe from {0} because no subscription was found fitting this criteria.", sid));
			return false;
		}
	}

	public boolean unsubscribeFromSerice(String servicePath) {
		Optional<Entry<String, Subscription>> subscriptionEntry = subscriptions.entrySet().parallelStream()
				.filter(entry -> entry.getValue().getServicePath().equals(servicePath)).findFirst();

		if (subscriptionEntry.isPresent()) {
			return unsubscribe(subscriptionEntry.get().getValue());
		} else {
			LOGGER.warning(MessageFormat.format(
					"Could not unsubscribe from {0} because no subscription was found fitting this criteria.",
					servicePath));
			return false;
		}
	}

	public boolean unsubscribe(Subscription subscription) {
		StringBuilder eventCancelation = new StringBuilder("UNSUBSCRIBE ").append(subscription.getServicePath())
				.append(" HTTP/1.1\r\n").append("HOST: ").append(deviceAddress.getHostAddress() + ":1400")
				.append("\r\n").append("SID: ").append(subscription.getToken()).append("\r\n\r\n");

		try (Socket socket = new Socket(deviceAddress, 1400)) {
			InputStream is = socket.getInputStream();

			// Send message
			OutputStream os = socket.getOutputStream();
			os.write(eventCancelation.toString().getBytes());
			os.flush();

			// Wait for confirmation
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String response = NetworkUtil.dumpReader(br);

			if (response.contains("200 OK")) {
				subscription.getRenewalFuture().cancel(false);
				subscriptions.remove(subscription.getToken());
				subscription.getEventListener().unsubscribed();
				return true;
			} else {
				// TODO failed to unsubscribe.
				LOGGER.severe(MessageFormat.format("failed to unsubscribe: {0}", response));

			}
		} catch (IOException io) {
			LOGGER.severe("failed to unsubscribe");
			LOGGER.severe(io.toString());
		}
		return false;
		// Response with 200 ok
	}

	/**
	 * UPnP Events create a new connection to the supplied callback socket every
	 * time an event occurs. Therefore, the socket is only valid for one event and
	 * can be safely discarded afterwards
	 */
	private Thread uPnPEventSocketListener = new DaemonThread(() -> {

		while (!Thread.interrupted() && !eventCallbackSocket.isClosed()) {
			// Await new events
			try {
				Socket eventSocket = eventCallbackSocket.accept();
				new Thread(() -> {
					parseUPnPEvent(eventSocket);
				}).start();
			}catch (IOException e) {
				
				if(e instanceof java.net.SocketException && e.getMessage().contains("closed")) {
					//Disregard. socket closed
					LOGGER.info("UPnPEvent socket closed");
				}else {
					e.printStackTrace();
				}
			} 
			catch (Exception exception) {
				// Bad practice but catch all issues like null pointer exceptions
				// which can unexpacetly happen during parsing if udp sends a bad request.
				// Prevent the entire
				// callback thread to break.
				LOGGER.severe(MessageFormat
						.format("An error occured during upnp event callback. Trying to recover: {0}", exception));
			}
		}

	}, "UPnP Event Socket Listener");

	private void parseUPnPEvent(Socket socket) {

		try {
			socket.setSoTimeout(300);

			String event = NetworkUtil.collectSocketWithTimeout(socket, 200);

			// Send aknowledgment
			OutputStream output = socket.getOutputStream();
			// Unescape xml
			if (StringUtil.isEscaped(event)) {
				event = StringEscapeUtils.unescapeXml(event);
			}

			LOGGER.fine("Event: " + event);

			if(event.length() > 0) {
				int indexXMLStart = event.indexOf("<e:propertyset");

				String headers = event.substring(0, indexXMLStart);
				String bodyContent = event.substring(indexXMLStart);
				String httpHeader = ParserHelper.findOne("(.*)\\R", headers);
				String host = ParserHelper.findOne("HOST: (.*)", headers);
				String connection = ParserHelper.findOne("CONNECTION: (.*)", headers);
				int contentLength = Integer.parseInt(ParserHelper.findOne("CONTENT-LENGTH: (.*)", headers));
				String nt = ParserHelper.findOne("NT: (.*)", headers);
				String nts = ParserHelper.findOne("NTS: (.*)", headers);
				String sid = ParserHelper.findOne("SID: (.*)", headers);
				String transferEncoding = ParserHelper.findOne("TRANSFER-ENCODING: \"(.*)\"", headers);
				int seq = Integer.parseInt(ParserHelper.findOne("SEQ: (.*)", headers));

				// Get the corresponding subscription object
				Subscription subscription = subscriptions.get(sid);

				if (subscription == null) {
					LOGGER.severe("Received UPnP event does not match any expected sid");
					return;
				}

				// Validation

				if (seq <= subscription.getSequenceCount()) {
					LOGGER.warning("UPnP Event arrived in wrong order.");
				} else {
					subscription.setSequenceCount(seq);
					;
				}

				// Chunked message?
				if (transferEncoding.contains("chunked")) {
					LOGGER.warning("implement chunk decoding");
				}

				SAXBuilder saxBuilder = new SAXBuilder();
				try {
					Document xml = saxBuilder.build(new StringReader(bodyContent));
					output.write(ACKNOWLEDGEMENT_MESSAGE);
					socket.getInputStream().close();
					output.close();
					socket.close();

					if (seq == 0) {
						UPnPEvent upnpEvent = new UPnPEvent(httpHeader, host, connection, contentLength, nt, nts, sid, seq,
								xml);
						subscription.getEventListener().initialEventReceived(upnpEvent);
					} else {
						UPnPEvent upnpEvent = new UPnPEvent(httpHeader, host, connection, contentLength, nt, nts, sid, seq,
								xml);
						subscription.getEventListener().eventReceived(upnpEvent);
					}

				} catch (JDOMException e) {
					output.write(BAD_REQUEST_MESSAGE);
					System.out.println("Malformed answer : " + bodyContent + "\n" + event);
					output.flush();
					socket.getInputStream().close();
					output.close();
					socket.close();
					LOGGER.severe(e.toString());
				}
			}else {
				LOGGER.warning("Dropped UDP package. Try again");
				output.write(BAD_REQUEST_MESSAGE);
				output.flush();
				socket.getInputStream().close();
				output.close();
				socket.close();
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shutdown non daemon threads ot the upnp device
	 */
        public void deinit() {
                if(scheduler != null) {
                        scheduler.shutdownNow();
                }
        }
	
	private Thread handleShutdown = new Thread(() -> {

		if (eventCallbackSocket != null && !eventCallbackSocket.isClosed()) {

			Iterator<Entry<String, Subscription>> keyIter = subscriptions.entrySet().iterator();

			while (keyIter.hasNext()) {
				Subscription subscription = keyIter.next().getValue();
				unsubscribe(subscription);
			}
			try {
				eventCallbackSocket.close();
				scheduler.shutdownNow();
			} catch (IOException e) {

			}
		}
	}, "UPnP Shutdown");

	/**
	 * Creates a dummy device pointing to the supplied ip address. This device can
	 * be used to subscribe to events but does not contain information usually
	 * transmitted via the SSDP advertisement.
	 * 
	 * @param ip Ip of the fake device
	 * @return A UPnPDevice pointing to the ip
	 * @throws UnknownHostException If the ip is not well formated or valid
	 */
	public static UPnPDevice createDummyDevice(String ip) throws UnknownHostException {
		return new UPnPDevice(InetAddress.getByName(ip), null);
	}

}
