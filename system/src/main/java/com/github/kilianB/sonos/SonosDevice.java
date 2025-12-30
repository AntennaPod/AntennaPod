package com.github.kilianB.sonos;

import com.github.kilianB.exception.SonosControllerException;
import com.github.kilianB.sonos.listener.AVTTransportListener;
import com.github.kilianB.sonos.listener.MediaRendererQueueListener;
import com.github.kilianB.sonos.listener.RenderingControlListener;
import com.github.kilianB.sonos.listener.SonosEventListener;
import com.github.kilianB.sonos.listener.ZoneTopologyListener;
import com.github.kilianB.sonos.model.AVTransportEvent;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.SonosSpeakerInfo;
import com.github.kilianB.sonos.model.SonosZoneInfo;
import com.github.kilianB.sonos.model.TrackInfo;
import com.github.kilianB.sonos.model.TrackMetadata;
import com.github.kilianB.uPnPClient.UPnPDevice;
import com.github.kilianB.uPnPClient.UPnPEvent;
import com.github.kilianB.uPnPClient.UPnPEventAdapterVerbose;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * @author vmichalak
 * @author Kilian
 *
 */
@SuppressWarnings("unused")
public class SonosDevice {

	/**
	 * The uPnPDevice instance created during the device advertisement.
	 */
	private UPnPDevice uPnPDevice;

	/**
	 * flag indicating if this devices is currently subscribed to the UPnP events
	 */
	private boolean uPnPSubscribed = false;

	/**
	 * UPnP standard Content Directory service, for functions relating to browsing,
	 * searching and listing available music
	 */

	private static final String MEDIA_SERVER_QUERY_EVENT_ENDPOINT = "/MediaServer/ContentDirectory/Event";

	/**
	 * UPnP standard connection manager service for the media server.
	 */
	private static final String MEDIA_SERVER_CONNECTION_EVENT_ENDPOINT = "/MediaServer/ConnectionManager/Event";

	/**
	 * UPnP standard rendering control service, for functions relating to playback
	 * rendering, eg bass, treble, volume and EQ
	 */
	private static final String MEDIA_RENDERER_CONTROL_EVENT_ENDPOINT = "/MediaRenderer/RenderingControl/Event";

	/**
	 * UPnP standard connection manager service for the media renderer.
	 */
	private static final String MEDIA_RENDERER_CONNECTION_EVENT_ENDPOINT = "/MediaRenderer/ConnectionManager/Event";

	/**
	 * UPnP standard AV Transport service, for functions relating to transport
	 * management, eg play, stop, seek, playlists etc
	 */
	private static final String MEDIA_RENDERER_AVTRANSPORT_EVENT_ENDPOINT = "/MediaRenderer/AVTransport/Event";

	/**
	 * Sonos queue service, for functions relating to queue management, saving
	 * queues etc
	 */
	private static final String MEDIA_RENDERER_QUEUE_EVENT_ENDPOINT = "/MediaRenderer/Queue/Event";

	/**
	 * Sonos group rendering control service, for functions relating to group volume
	 * etc
	 */
	private static final String MEDIA_RENDERER_GROUP_CONTROL_EVENT_ENDPOINT = "/MediaRenderer/GroupRenderingControl/Event";

	/**
	 * Sonos topology service relating to connection, disconnecting grouping and
	 * ungrouping of zones
	 */
	private static final String ZONE_GROUP_TOPOLOGY_EVENT_ENDPOINT = "/ZoneGroupTopology/Event";

	/**
	 * Unknown
	 */
	private static final String SYSTEM_PROPERTIES_EVENT_ENDPOINT = "/SystemProperties/Event";
	/**
	 * Unknown
	 */
	private static final String SYSTEM_PROPERTIES_CONTROL_ENDPOINT = "/SystemProperties/Control";

	/**
	 * Alarm clock service endpoint. Upon change this points towards a new list with
	 * the changes. TODO see how we can get a hold on the information once we get
	 * the updated list id
	 */
	private static final String ALARM_CLOCK_EVENT_ENDPONT = "/AlarmClock/Event";
	/**
	 * Unknown
	 */
	private static final String MUSIC_SERVICE_EVENT_ENDPOINT = "/MusicServices/Event";

	/**
	 * The ip address of the sonos device
	 */
	private final String ip;

	/**
	 * Sonos event handler being called upon state change
	 */
	private List<SonosEventListener> sonosEventHandlers = new ArrayList<SonosEventListener>();

	private List<String> eventSubscriptions = new ArrayList<String>();

	// Cache some information which are likely to stay the same
	private String roomName;
	private String deviceName;

	public SonosDevice(UPnPDevice device) {
		this.uPnPDevice = device;
		this.ip = device.getIP().getHostAddress();
	}

	/**
	 * Create a sonos device from a known ip. A dummy uPnP device will be created
	 * without containing information from SDDP advertisement information. The sonos
	 * device can most likely be used without any loss of functionality but it is
	 * still recommended to use the constructor {@link #SonosDevice(UPnPDevice)};
	 * 
	 * @param ip The ip address of the sonos device
	 * @throws UnknownHostException If the supplied ip is not well formated or does
	 *                              not point to a valid device
	 */
	@Deprecated
	public SonosDevice(String ip) throws UnknownHostException {
		this.uPnPDevice = UPnPDevice.createDummyDevice(ip);
		this.ip = ip;
	}

	/**
	 * Gets the remote IP address of this device.
	 * 
	 * @return Returns the remote IP address of the device.
	 */
	public String getIpAddress() {
		return this.ip;
	}

	// <editor-fold desc="AV TRANSPORT">

	/**
	 * Play the currently selected track.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void play() throws IOException, SonosControllerException {
		CommandBuilder.transport("Play").put("InstanceID", "0").put("Speed", "1").executeOn(this.ip);
	}

	/**
	 * Play a given stream. Pauses the queue.
	 * 
	 * @param uri      URI of a stream to be played.
	 * @param metadata The track metadata to show in the player (DIDL format).
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void playUri(String uri, TrackMetadata metadata) throws IOException, SonosControllerException {
		String metadataString = "";
		if (metadata != null) {
			metadataString = metadata.toDIDL();
		}
		System.out.println("Play uri: " + uri);
		CommandBuilder.transport("SetAVTransportURI").put("InstanceID", "0").put("CurrentURI", uri)
				.put("CurrentURIMetaData", metadataString).executeOn(this.ip);

		this.play();
	}

	/**
	 * Play an item from the queue.
	 * 
	 * @param queueIndex The index of the item in the queue. Starting at 1
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws IllegalArgumentException if queue index {@literal <} 1
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	
	public void playFromQueue(int queueIndex) throws IOException, SonosControllerException {
		if (queueIndex < 1) {
			throw new IllegalArgumentException("Queue index cannot be < 1.");
		}
		this.playUri("x-rincon-queue:" + this.getSpeakerInfo().getLocalUID() + "#0", null);
		CommandBuilder.transport("Seek").put("InstanceID", "0").put("Unit", "TRACK_NR")
				.put("Target", String.valueOf(queueIndex)).executeOn(this.ip);
		this.play();
	}

	/**
	 * Pause current music, Play URI and resume (very useful for announcement). clip
	 * is a blocking method. Take care !
	 * 
	 * @param uri      URI of a stream to be played.
	 * @param metadata The track metadata to show in the player (DIDL format).
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 * @throws InterruptedException     If the thread gets interrupted
	 */
	public synchronized void clip(String uri, TrackMetadata metadata)
			throws IOException, SonosControllerException, InterruptedException {
		PlayState previousState = this.getPlayState();
		TrackInfo previous = this.getCurrentTrackInfo();
		this.playUri(uri, metadata);

		// Fix. If list is empty this will throw an error

		// TODO I don't like vmichalak implementation. Can we resolve this without
		// polling? Maybe even use the upnp listener track changed?
		while (!this.getPlayState().equals(PlayState.STOPPED)) {
			Thread.sleep(500);
		}
	
		if (!this.getQueue(0,1).isEmpty()) {
			this.playUri("x-rincon-queue:" + this.getSpeakerInfo().getLocalUID() + "#0", null);
			CommandBuilder.transport("Seek").put("InstanceID", "0").put("Unit", "TRACK_NR")
					.put("Target", String.valueOf(previous.getQueueIndex())).executeOn(this.ip);
			this.seek(previous.getPosition());
			if (previousState.equals(PlayState.PLAYING)) {
				this.play();
			} else {
				this.pause();
			}
		}
	}

	/**
	 * Pause the currently playing track.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void pause() throws IOException, SonosControllerException {
		CommandBuilder.transport("Pause").put("InstanceID", "0").put("Speed", "1").executeOn(this.ip);
	}

	/**
	 * Get the play state of the device.
	 * 
	 * @return current PlayState of the device
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public PlayState getPlayState() throws IOException, SonosControllerException {
		String r = CommandBuilder.transport("GetTransportInfo").put("InstanceID", "0").executeOn(this.ip);
		return PlayState.valueOf(ParserHelper.findOne("<CurrentTransportState>(.*)</CurrentTransportState>", r));
	}

	/**
	 * Stop the currently playing track.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void stop() throws IOException, SonosControllerException {
		CommandBuilder.transport("Stop").put("InstanceID", "0").put("Speed", "1").executeOn(this.ip);
	}

	/**
	 * Seeks to a given timestamp in the current track, specified in the format
	 * HH:MM:SS.
	 * 
	 * @param time specified in the format HH:MM:SS.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void seek(String time) throws IOException, SonosControllerException {
		CommandBuilder.transport("Seek").put("InstanceID", "0").put("Unit", "REL_TIME").put("Target", time)
				.executeOn(this.ip);
	}

	public void seek(int time) throws IOException, SonosControllerException {
		seek(ParserHelper.secondsToFormatedTimestamp(time));
	}

	/**
	 * Go to the next track on the queue.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void next() throws IOException, SonosControllerException {
		CommandBuilder.transport("Next").put("InstanceID", "0").put("Speed", "1").executeOn(this.ip);
	}

	/**
	 * Go back to the previously played track.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void previous() throws IOException, SonosControllerException {
		CommandBuilder.transport("Previous").put("InstanceID", "0").put("Speed", "1").executeOn(this.ip);
	}

	/**
	 * Adds a given track to the queue.
	 * 
	 * @param uri      URI of a stream to be played.
	 * @param metadata The track metadata to show in the player (DIDL format).
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void addToQueue(String uri, TrackMetadata metadata) throws IOException, SonosControllerException {
		String metadataString = "";
		if (metadata != null) {
			metadataString = metadata.toDIDL();
		}
		CommandBuilder.transport("AddURIToQueue").put("InstanceID", "0").put("EnqueuedURI", uri)
				.put("EnqueuedURIMetaData", metadataString).put("DesiredFirstTrackNumberEnqueued", "0")
				.put("EnqueueAsNext", "1").executeOn(this.ip);
	}

	/**
	 * Adds a given track to the queue.
	 * 
	 * @param queueIndex the index of the queue the track to shall be inserted
	 * @param uri        URI of a stream to be played.
	 * @param metadata   The track metadata to show in the player (DIDL format).
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void addToQueue(int queueIndex, String uri, TrackMetadata metadata)
			throws IOException, SonosControllerException {
		String metadataString = "";
		if (metadata != null) {
			metadataString = metadata.toDIDL();
		}
		CommandBuilder.transport("AddURIToQueue").put("InstanceID", "0").put("EnqueuedURI", uri)
				.put("EnqueuedURIMetaData", metadataString)
				.put("DesiredFirstTrackNumberEnqueued", Integer.toString(queueIndex)).put("EnqueueAsNext", "0")
				.executeOn(this.ip);
	}

	/**
	 * Remove a track from the queue.
	 * 
	 * @param queueIndex index of the item to remove from the queue. Has to be
	 *                   greater than 0
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void removeFromQueue(int queueIndex) throws IOException, SonosControllerException {
		if (queueIndex < 0) {
			throw new IllegalArgumentException("Queue index cannot be < 0.");
		}
		CommandBuilder.transport("RemoveTrackFromQueue").put("InstanceID", "0").put("ObjectID", "Q:0/" + queueIndex)
				.put("UpdateID", "0").executeOn(this.ip);
	}

	/**
	 * Get Current Track Info (position in the queue, duration, position, ...).
	 * 
	 * @return TrackInfo object.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public TrackInfo getCurrentTrackInfo() throws IOException, SonosControllerException {
		String r = CommandBuilder.transport("GetPositionInfo").put("InstanceID", "0").put("Channel", "Master")
				.executeOn(this.ip);
		String track = ParserHelper.findOne("<Track>([0-9]*)</Track>", r);
		int trackNumber = -1;
		if (!track.equals("NOT_IMPLEMENTED") && !track.equals("")) {
			trackNumber = Integer.valueOf(track);
		}
		return new TrackInfo(trackNumber,
				ParserHelper.formatedTimestampToSeconds(
						ParserHelper.findOne("<TrackDuration>([0-9]*:[0-9]*:[0-9]*)</TrackDuration>", r)),
				ParserHelper.formatedTimestampToSeconds(
						ParserHelper.findOne("<RelTime>([0-9]*:[0-9]*:[0-9]*)</RelTime>", r)),
				ParserHelper.findOne("<TrackURI>(.*)</TrackURI>", r),
				TrackMetadata.parse(ParserHelper.findOne("<TrackMetaData>(.*)</TrackMetaData>", r)));
	}

	/**
	 * Get the play mode for the queue.
	 * 
	 * @return current PlayMode of the queue
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public PlayMode getPlayMode() throws IOException, SonosControllerException {
		String r = CommandBuilder.transport("GetTransportSettings").put("InstanceID", "0").executeOn(this.ip);
		return PlayMode.valueOf(ParserHelper.findOne("<PlayMode>(.*)</PlayMode>", r));
	}

	/**
	 * Sets the play mode for the queue.
	 * 
	 * @param playMode New play mode
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setPlayMode(PlayMode playMode) throws IOException, SonosControllerException {
		CommandBuilder.transport("SetPlayMode").put("InstanceID", "0").put("NewPlayMode", playMode.toString())
				.executeOn(this.ip);
	}

	/**
	 * Remove all tracks from the queue.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void clearQueue() throws IOException, SonosControllerException {
		CommandBuilder.transport("RemoveAllTracksFromQueue").put("InstanceID", "0").executeOn(this.ip);
	}

	/**
	 * Return if the Sonos is joined with another one.
	 * 
	 * @return True if is joined, false if is isn't
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public boolean isJoined() throws IOException, SonosControllerException {
		return this.getZoneGroupState().getZonePlayerUIDInGroup().size() > 1;
	}

	/**
	 * Get all Sonos speaker joined with this speaker.
	 * 
	 * @return List of Sonos speaker joined with this speaker.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public List<SonosDevice> joinedWith() throws IOException, SonosControllerException {
		return this.getZoneGroupState().getSonosDevicesInGroup();
	}

	/**
	 * Join this Sonos speaker to another.
	 * 
	 * @param master master speaker
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void join(SonosDevice master) throws IOException, SonosControllerException {
		this.join(master.getSpeakerInfo().getLocalUID());
	}

	/**
	 * Join this Sonos speaker to another.
	 * 
	 * @param masterUID master speaker UID
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void join(String masterUID) throws IOException, SonosControllerException {
		CommandBuilder.transport("SetAVTransportURI").put("InstanceID", "0").put("CurrentURI", "x-rincon:" + masterUID)
				.put("CurrentURIMetaData", "").executeOn(this.ip);
	}

	/**
	 * Remove this speaker from a group.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void unjoin() throws IOException, SonosControllerException {
		CommandBuilder.transport("BecomeCoordinatorOfStandaloneGroup").put("InstanceID", "0").put("Speed", "1")
				.executeOn(this.ip);
	}

	/**
	 * Switch the speaker's input to line-in.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void switchToLineIn() throws IOException, SonosControllerException {
		String uid = this.getSpeakerInfo().getLocalUID();
		CommandBuilder.transport("SetAVTransportURI").put("InstanceID", "0").put("CurrentURI", "x-rincon-stream:" + uid)
				.put("CurrentURIMetaData", "").executeOn(this.ip);
	}

	/**
	 * Switch the speaker's input to TV input. /!\ WARNING: WORKS ONLY WITH PLAYBAR
	 * / PLAYBASE /!\
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void switchToTV() throws IOException, SonosControllerException {
		String uid = this.getSpeakerInfo().getLocalUID();
		CommandBuilder.transport("SetAVTransportURI").put("InstanceID", "0")
				.put("CurrentURI", "x-sonos-htastream:" + uid + ":spdif").put("CurrentURIMetaData", "")
				.executeOn(this.ip);
	}

	/**
	 * Get the Sonos speaker volume.
	 * 
	 * @return A volume value between 0 and 100
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public int getVolume() throws IOException, SonosControllerException {
		String r = CommandBuilder.rendering("GetVolume").put("InstanceID", "0").put("Channel", "Master")
				.executeOn(this.ip);
		return Integer.parseInt(ParserHelper.findOne("<CurrentVolume>([0-9]*)</CurrentVolume>", r));
	}

	/**
	 * Set the Sonos speaker volume.
	 * 
	 * @param volume A volume value between 0 and 100
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setVolume(int volume) throws IOException, SonosControllerException {
		CommandBuilder.rendering("SetVolume").put("InstanceID", "0").put("Channel", "Master")
				.put("DesiredVolume", String.valueOf(volume)).executeOn(this.ip);
	}

	/**
	 * Return the mute state of the Sonos speaker.
	 * 
	 * @return True if is muted, false if isn't
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public boolean isMuted() throws IOException, SonosControllerException {
		String r = CommandBuilder.rendering("GetMute").put("InstanceID", "0").put("Channel", "Master")
				.executeOn(this.ip);
		return ParserHelper.findOne("<CurrentMute>([01])</CurrentMute>", r).equals("1") ? true : false;
	}

	/**
	 * Mute or unmute the Sonos speaker.
	 * 
	 * @param state True to mute, False to unmute
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setMute(boolean state) throws IOException, SonosControllerException {
		CommandBuilder.rendering("SetMute").put("InstanceID", "0").put("Channel", "Master")
				.put("DesiredMute", state ? "1" : "0").executeOn(this.ip);
	}

	/**
	 * Mute or unmute the speaker.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void switchMute() throws IOException, SonosControllerException {
		setMute(!isMuted());
	}

	/**
	 * Get the Sonos speaker bass EQ.
	 * 
	 * @return value between 10 and -10
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public int getBass() throws IOException, SonosControllerException {
		String r = CommandBuilder.rendering("GetBass").put("InstanceID", "0").put("Channel", "Master")
				.executeOn(this.ip);
		return Integer.parseInt(ParserHelper.findOne("<CurrentBass>(.*)</CurrentBass>", r));
	}

	/**
	 * Set the Sonos speaker bass EQ.
	 * 
	 * @param bass Value between 10 and -10
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setBass(int bass) throws IOException, SonosControllerException {
		if (bass > 10 || bass < -10) {
			throw new IllegalArgumentException("Bass value need to be between 10 and -10");
		}
		CommandBuilder.rendering("SetBass").put("InstanceID", "0").put("DesiredBass", String.valueOf(bass))
				.executeOn(this.ip);
	}

	/**
	 * Get the Sonos speaker's loudness compensation.
	 * 
	 * @return True if is On, False if isn't
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public boolean isLoudnessActivated() throws IOException, SonosControllerException {
		String r = CommandBuilder.rendering("GetLoudness").put("InstanceID", "0").put("Channel", "Master")
				.executeOn(this.ip);
		return ParserHelper.findOne("<CurrentLoudness>(.*)</CurrentLoudness>", r).equals("1") ? true : false;
	}

	/**
	 * Set the Sonos speaker's loudness compensation.
	 * 
	 * @param loudness True for set On, False for set Off
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setLoudness(boolean loudness) throws IOException, SonosControllerException {
		CommandBuilder.rendering("SetLoudness").put("InstanceID", "0").put("Channel", "Master")
				.put("DesiredLoudness", loudness ? "1" : "0").executeOn(this.ip);
	}

	/**
	 * Get the Sonos speaker's treble EQ.
	 * 
	 * @return value between -10 and 10
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public int getTreble() throws IOException, SonosControllerException {
		String r = CommandBuilder.rendering("GetTreble").put("InstanceID", "0").put("Channel", "Master")
				.executeOn(this.ip);
		return Integer.parseInt(ParserHelper.findOne("<CurrentTreble>(.*)</CurrentTreble>", r));
	}

	/**
	 * Set the Sonos speaker's treble EQ.
	 * 
	 * @param treble value between -10 and 10
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setTreble(int treble) throws IOException, SonosControllerException {
		if (treble > 10 || treble < -10) {
			throw new IllegalArgumentException("treble value need to be between 10 and -10");
		}
		CommandBuilder.rendering("SetTreble").put("InstanceID", "0").put("DesiredTreble", String.valueOf(treble))
				.executeOn(this.ip);
	}

	/**
	 * Check if the Night Mode is activated or not. /!\ WARNING: WORKS ONLY WITH
	 * PLAYBAR / PLAYBASE /!\
	 * 
	 * @return True if activated, False if isn't.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public boolean isNightModeActivated() throws IOException, SonosControllerException {
		String s = CommandBuilder.rendering("GetEQ").put("InstanceID", "0").put("EQType", "NightMode")
				.executeOn(this.ip);
		return ParserHelper.findOne("<CurrentValue>(.*)</CurrentValue>", s).equals("1") ? true : false;
	}

	/**
	 * Set the Night Mode. /!\ WARNING: WORKS ONLY WITH PLAYBAR / PLAYBASE /!\
	 * 
	 * @param state new night mode
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setNightMode(boolean state) throws IOException, SonosControllerException {
		CommandBuilder.rendering("SetEQ").put("InstanceID", "0").put("EQType", "NightMode")
				.put("DesiredValue", state ? "1" : "0").executeOn(this.ip);
	}

	/**
	 * Turn On / Off the Night Mode.
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void switchNightMode() throws IOException, SonosControllerException {
		this.setNightMode(!this.isNightModeActivated());
	}

	/**
	 * Check if the Dialog Mode is activated or not. /!\ WARNING: WORKS ONLY WITH
	 * PLAYBAR / PLAYBASE /!\
	 * 
	 * @return True if activated, False if isn't.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public boolean isDialogModeActivated() throws IOException, SonosControllerException {
		String s = CommandBuilder.rendering("GetEQ").put("InstanceID", "0").put("EQType", "DialogLevel")
				.executeOn(this.ip);
		return ParserHelper.findOne("<CurrentValue>(.*)</CurrentValue>", s).equals("1") ? true : false;
	}

	/**
	 * Set the Dialog Mode. /!\ WARNING: WORKS ONLY WITH PLAYBAR / PLAYBASE /!\
	 * 
	 * @param state new dialog mode
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void setDialogMode(boolean state) throws IOException, SonosControllerException {
		CommandBuilder.rendering("SetEQ").put("InstanceID", "0").put("EQType", "DialogLevel")
				.put("DesiredValue", state ? "1" : "0").executeOn(this.ip);
	}

	/**
	 * Turn On / Off the Night Mode. /!\ WARNING: WORKS ONLY WITH PLAYBAR / PLAYBASE
	 * /!\
	 * 
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public void switchDialogMode() throws IOException, SonosControllerException {
		this.setDialogMode(!this.isDialogModeActivated());
	}

	// </editor-fold>

	// <editor-fold desc="DEVICE">

	/**
	 * Get the zone name. (for exemple: "Bedroom + 1", "Living Room", ...)
	 * 
	 * @return the zone name.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public String getZoneName() throws IOException, SonosControllerException {
		return this.getZoneGroupState().getName();
	}

	/**
	 * Get the room name of the sonos speaker. This method caches the result and
	 * will not be aware of changes until the {@link #getRoomName()} method was
	 * called at least once.
	 * <p>
	 * This method hides the usual IO and SonosControler Exceptions for convenience
	 * sake. To ensure you got a prober result the first return value should be
	 * checked against null.
	 * 
	 * @return the cached room name of the device or null if the room name wasn't
	 *         retrieved at least once
	 */
	public String getRoomNameCached() {
		if (roomName == null) {
			try {
				getRoomName();
			} catch (IOException | SonosControllerException e) {
				// The cached call should not require to handle exceptions all the time.
				LOGGER.severe(MessageFormat.format("Could not retrieve the room name of the device: {0}", e));
			}
		}
		return roomName;
	}

	/**
	 * Get the room name. (for exemple: "Bedroom", "Living Room", ...)
	 *
	 * @return the room name.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public String getRoomName() throws IOException, SonosControllerException {
		String r = CommandBuilder.download(this.ip, "xml/device_description.xml");
		r = Pattern.compile("<deviceList>.*</deviceList>", Pattern.DOTALL).matcher(r).replaceFirst("");
		String roomName = ParserHelper.findOne("<roomName>(.*)</roomName>", r);
		this.roomName = roomName;
		return roomName;
	}

	/**
	 * Get the device name of the sonos speaker. This method caches the result and
	 * will not be aware of changes until the {@link #getDeviceName()} method was
	 * called at least once.
	 * <p>
	 * This method hides the usual IO and SonosControler Exceptions for convenience
	 * sake. To ensure you got a prober result the first return value should be
	 * checked against null.
	 *
	 * @return the cached room name of the device or null if the room name wasn't
	 *         retrieved at least once
	 */
	public String getDeviceNameCached() {
		if (deviceName == null) {
			try {
				getDeviceName();
			} catch (IOException | SonosControllerException e) {
				// The cached call should not require to handle exceptions all the time.
				LOGGER.severe(MessageFormat.format("Could not retrieve the device name: {0}", e));
			}
		}
		return deviceName;
	}

	/**
	 * Get the device name. (for exemple: "Bedroom (L)", "Bedroom (R)", ...)
	 * 
	 * @return the device name.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public String getDeviceName() throws IOException, SonosControllerException {
		String deviceName = this.getSpeakerInfo().getDeviceName();
		this.deviceName = deviceName;
		return deviceName;
	}

	public void setRoomName(String roomName) throws IOException, SonosControllerException {
		CommandBuilder.device("SetZoneAttributes").put("DesiredZoneName", roomName).put("DesiredIcon", "")
				.put("DesiredConfiguration", "").executeOn(this.ip);
		this.roomName = roomName;
	}

	public boolean getLedState() throws IOException, SonosControllerException {
		String r = CommandBuilder.device("GetLEDState").executeOn(this.ip);
		return ParserHelper.findOne("<CurrentLEDState>(.*)</CurrentLEDState>", r).equals("On") ? true : false;
	}

	public void setLedState(boolean state) throws IOException, SonosControllerException {
		CommandBuilder.device("SetLEDState").put("DesiredLEDState", state ? "On" : "Off").executeOn(this.ip);
	}

	public void switchLedState() throws IOException, SonosControllerException {
		setLedState(!getLedState());
	}

	public List<TrackMetadata> getQueue(int startingIndex, int requestedCount)
			throws IOException, SonosControllerException {
		String r = CommandBuilder.contentDirectory("Browse").put("ObjectID", "Q:0")
				.put("BrowseFlag", "BrowseDirectChildren")
				.put("Filter", "dc:title,res,dc:creator,upnp:artist,upnp:album,upnp:albumArtURI")
				.put("StartingIndex", String.valueOf(startingIndex))
				.put("RequestedCount", String.valueOf(requestedCount)).put("SortCriteria", "").executeOn(this.ip);
		List<String> itemsNonParsed = ParserHelper.findAll("<item .+?(?=>)>(.+?(?=</item>))", r);
		List<TrackMetadata> itemsParsed = new ArrayList<TrackMetadata>();
		for (String s : itemsNonParsed) {
			itemsParsed.add(TrackMetadata.parse(s));
		}
		return itemsParsed;
	}

	public SonosZoneInfo getZoneGroupState() throws IOException, SonosControllerException {
		String r = CommandBuilder.zoneGroupTopology("GetZoneGroupAttributes").executeOn(this.ip);
		String name = ParserHelper.findOne("<CurrentZoneGroupName>(.*)</CurrentZoneGroupName>", r);
		String id = ParserHelper.findOne("<CurrentZoneGroupID>(.*)</CurrentZoneGroupID>", r);
		String devices = ParserHelper.findOne("<CurrentZonePlayerUUIDsInGroup>(.*)</CurrentZonePlayerUUIDsInGroup>", r);
		List<String> deviceList = Arrays.asList(devices.split(","));
		return new SonosZoneInfo(name, id, deviceList);
	}

	/**
	 * Check if the speaker is a group coordinator or not.
	 * 
	 * @return True if the speaker is a group coordinator, otherwise False.
	 * @throws IOException              IOException during HTTP Client operation .
	 *                                  Sending the command.
	 * @throws SonosControllerException UPnP Error returned by the device
	 */
	public boolean isCoordinator() throws IOException, SonosControllerException {
		// If zone have the same UID as the speaker -> speaker is the coordinator of the
		// zone.
		String uid = this.getSpeakerInfo().getLocalUID();
		return getZoneGroupState().getId().startsWith(uid);
	}

	//@formatter:off
    /**
     * Get information about the Sonos speaker.
     * @return Information about the Sonos speaker, such as the UID, MAC Address, and Zone Name.
     * @throws IOException	IOException during HTTP Client operation . Sending the command.
	 * @throws SonosControllerException	 UPnP Error returned by the device
     */
	public SonosSpeakerInfo getSpeakerInfo() throws IOException, SonosControllerException {
		String responseString = CommandBuilder.download(ip, "status/zp");

		String deviceName				= ParserHelper.findOne("<ZoneName>(.*)</ZoneName>", responseString);
		String zoneIcon					= ParserHelper.findOne("<ZoneIcon>(.*)</ZoneIcon>", responseString);
		String configuration			= ParserHelper.findOne("<Configuration>(.*)</Configuration>", responseString);
		String localUID					= ParserHelper.findOne("<LocalUID>(.*)</LocalUID>", responseString);
		String serialNumber				= ParserHelper.findOne("<SerialNumber>(.*)</SerialNumber>", responseString);
		String softwareVersion			= ParserHelper.findOne("<SoftwareVersion>(.*)</SoftwareVersion>",
											responseString);
		String softwareDate				= ParserHelper.findOne("<SoftwareDate>(.*)</SoftwareDate>", responseString);
		String softwareScm				= ParserHelper.findOne("<SoftwareScm>(.*)</SoftwareScm>", responseString);
		String minCompatibleVersion 	= ParserHelper.findOne("<MinCompatibleVersion>(.*)</MinCompatibleVersion>",
											responseString);
		String legacyCompatibleVersion	= ParserHelper.findOne("<LegacyCompatibleVersion>(.*)</LegacyCompatibleVersion>",
											responseString);
		String hardwareVersion			= ParserHelper.findOne("<HardwareVersion>(.*)</HardwareVersion>",
											responseString);
		String dspVersion				= ParserHelper.findOne("<DspVersion>(.*)</DspVersion>", responseString);
		String hwFlags					= ParserHelper.findOne("<HwFlags>(.*)</HwFlags>", responseString);
		String hwFeatures				= ParserHelper.findOne("<HwFeatures>(.*)</HwFeatures>", responseString);
		String variant					= ParserHelper.findOne("<Variant>(.*)</Variant>", responseString);
		String generalFlags				= ParserHelper.findOne("<GeneralFlags>(.*)</GeneralFlags>", responseString);
		String ipAddress				= ParserHelper.findOne("<IPAddress>(.*)</IPAddress>", responseString);
		String macAddress				= ParserHelper.findOne("<MACAddress>(.*)</MACAddress>", responseString);
		String copyright				= ParserHelper.findOne("<Copyright>(.*)</Copyright>", responseString);
		String extraInfo				= ParserHelper.findOne("<ExtraInfo>(.*)</ExtraInfo>", responseString);
		String htAudioInCode			= ParserHelper.findOne("<HTAudioInCode>(.*)</HTAudioInCode>", responseString);
		String idxTrk					= ParserHelper.findOne("<IdxTrk>(.*)</IdxTrk>", responseString);
		String mdp2Ver					= ParserHelper.findOne("<MDP2Ver>(.*)</MDP2Ver>", responseString);
		String mdp3Ver					= ParserHelper.findOne("<MDP3Ver>(.*)</MDP3Ver>", responseString);
		String relBuild					= ParserHelper.findOne("<RelBuild>(.*)</RelBuild>", responseString);
		String whitelistBuild			= ParserHelper.findOne("<WhitelistBuild>(.*)</WhitelistBuild>", responseString);
		String prodUnit					= ParserHelper.findOne("<ProdUnit>(.*)</ProdUnit>", responseString);
		String fuseCfg					= ParserHelper.findOne("<FuseCfg>(.*)</FuseCfg>", responseString);
		String revokeFuse				= ParserHelper.findOne("<RevokeFuse>(.*)</RevokeFuse>", responseString);
		String authFlags				= ParserHelper.findOne("<AuthFlags>(.*)</AuthFlags>", responseString);
		String swFeatures				= ParserHelper.findOne("<SwFeatures>(.*)</SwFeatures>", responseString);
		String regState					= ParserHelper.findOne("<RegState>(.*)</RegState>", responseString);
		String customerID				= ParserHelper.findOne("<CustomerID>(.*)</CustomerID>", responseString);

		return new SonosSpeakerInfo(deviceName, zoneIcon, configuration, localUID, serialNumber, softwareVersion,
				softwareDate, softwareScm, minCompatibleVersion, legacyCompatibleVersion, hardwareVersion, dspVersion,
				hwFlags, hwFeatures, variant, generalFlags, ipAddress, macAddress, copyright, extraInfo, htAudioInCode,
				idxTrk, mdp2Ver, mdp3Ver, relBuild, whitelistBuild, prodUnit, fuseCfg, revokeFuse, authFlags,
				swFeatures, regState, customerID);
	}

	//@formatter:on

	@Override
	public String toString() {
		return "SonosDevice{" + "ip='" + ip + '\'' + " name='" + this.getDeviceNameCached() + '\'' + '}';
	}

	/*
	 * Subscription api
	 */

	/**
	 * Register an event handler to listen to events emitted by this device
	 * 
	 * @param eventHandler the event handler to process events
	 * @return true if the event handler was sucessfully registered false if it
	 *         failed. This might happen due to IOException when subscribing to the
	 *         underlying UPnP Events
	 */
	public boolean registerSonosEventListener(SonosEventListener eventHandler) {

		if (!sonosEventHandlers.contains(eventHandler)) {
			boolean sucess = sonosEventHandlers.add(eventHandler);
			if (!uPnPSubscribed) {
				subscribeToUPnPEvents();
			}
			return sucess;
		} else {
			LOGGER.fine(MessageFormat.format("Event listener {0} already registered", eventHandler));
			return false;
		}
	}

	/**
	 * Remove an event handler from the notification list.
	 * 
	 * @param eventHandler The event handler who no langer shall receive events
	 * @return true if the event handler was sucessfully removed from the device
	 *         false if the event handler was not present in the first place
	 */
	public boolean unregisterSonosEventListener(SonosEventListener eventHandler) {
		if (sonosEventHandlers.contains(eventHandler)) {
			sonosEventHandlers.remove(eventHandler);
			if (sonosEventHandlers.size() == 0 && uPnPSubscribed) {
				unsubscribeUPnPEvents();
			}
			return true;

		} else {
			LOGGER.fine(
					MessageFormat.format("Trying to remove event listener {0} but it's not registered", eventHandler));
			return false;
		}
	}

	@Deprecated
	/**
	 * Get the uPnPDevice backing this sonos controller. Calling this method usually
	 * is not necessary and there is mostly no reason to access the underlying
	 * device.
	 * 
	 * @return the upnp device
	 */
	public UPnPDevice getUPnPDevice() {
		return uPnPDevice;
	}

	private void subscribeToUPnPEvents() {
		// Subscribe to all events
		try {
			eventSubscriptions
					.add(uPnPDevice.subscribe(new RenderingControlListener(MEDIA_RENDERER_CONTROL_EVENT_ENDPOINT, this),
							MEDIA_RENDERER_CONTROL_EVENT_ENDPOINT));
			eventSubscriptions
					.add(uPnPDevice.subscribe(new AVTTransportListener(MEDIA_RENDERER_AVTRANSPORT_EVENT_ENDPOINT, this),
							MEDIA_RENDERER_AVTRANSPORT_EVENT_ENDPOINT));
			eventSubscriptions
					.add(uPnPDevice.subscribe(new MediaRendererQueueListener(MEDIA_RENDERER_QUEUE_EVENT_ENDPOINT, this),
							MEDIA_RENDERER_QUEUE_EVENT_ENDPOINT));
			eventSubscriptions
					.add(uPnPDevice.subscribe(new ZoneTopologyListener(ZONE_GROUP_TOPOLOGY_EVENT_ENDPOINT, this),
							ZONE_GROUP_TOPOLOGY_EVENT_ENDPOINT));
			// uPnPDevice.subscribe(new UPnPEventAdapterVerbose(ALARM_CLOCK_EVENT_ENDPONT),
			// ALARM_CLOCK_EVENT_ENDPONT);
			// uPnPDevice.subscribe(new
			// UPnPEventAdapterVerbose(MEDIA_RENDERER_GROUP_CONTROL_EVENT_ENDPOINT),MEDIA_RENDERER_GROUP_CONTROL_EVENT_ENDPOINT);

			uPnPSubscribed = true;
		} catch (IOException io) {
			io.printStackTrace();
		}

	}

	private void unsubscribeUPnPEvents() {
		ListIterator<String> eventKeys = eventSubscriptions.listIterator();

		while (eventKeys.hasNext()) {
			String sid = eventKeys.next();
			if (uPnPDevice.unsubscribeFromToken(sid)) {
				eventKeys.remove();
			} else {
				LOGGER.warning("Could not unsubscreibe from " + sid);
			}
		}
		uPnPSubscribed = false;
	}

	public List<SonosEventListener> getEventListener() {
		return sonosEventHandlers;
	}

	public String resolveAlbumURL(String baseURL) {
		return "http://" + ip + ":1400" + baseURL;
	}

	/**
	 * Shuts down all non daemon threads associated with this device after
	 * registering event handlers. This will lead to jvm being able to orderly
	 * shutdown.
	 */
	public void deinit() {
		uPnPDevice.deinit();
	}

	private static final Logger LOGGER = Logger.getLogger(SonosDevice.class.getName());

}
