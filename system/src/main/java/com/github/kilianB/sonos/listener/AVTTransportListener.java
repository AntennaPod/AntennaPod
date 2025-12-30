package com.github.kilianB.sonos.listener;

import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.github.kilianB.sonos.ParserHelper;
import com.github.kilianB.sonos.SonosDevice;
import com.github.kilianB.sonos.model.AVTransportEvent;
import com.github.kilianB.sonos.model.PlayMode;
import com.github.kilianB.sonos.model.PlayState;
import com.github.kilianB.sonos.model.TrackInfo;
import com.github.kilianB.sonos.model.TrackMetadata;
import com.github.kilianB.uPnPClient.UPnPEvent;
import com.github.kilianB.uPnPClient.UPnPEventAdapter;
import com.github.kilianB.uPnPClient.UPnPEventAdapterVerbose;

/**
 * Event listener used to parse UPnPEvents received from the AVTTransport service relating to transport
 * management, eg play, stop, seek, playlists etc
 * @author Kilian
 *
 */
public class AVTTransportListener extends UPnPEventAdapter {

	private static final Namespace upnpAVTNamespace = Namespace.getNamespace("urn:schemas-upnp-org:metadata-1-0/AVT/");
	private static final  Namespace upnpRinnconnectsNamespace =  Namespace.getNamespace("r","urn:schemas-rinconnetworks-com:metadata-1-0/");
	
	/**
	 * Event listeners to be notified in case of noteworthy events
	 */
	private final List<SonosEventListener> listeners;

	//Keep an internal state so we can notify listeners in case of changes
	private TrackInfo currentTrack;
	private PlayMode  currentPlayMode;
	private PlayState currentPlayState;
	
	public AVTTransportListener(String servicePath, SonosDevice device) {
		//super(servicePath);
		this.listeners = device.getEventListener();
	}

	@Override
	public void initialEventReceived(UPnPEvent event) {
		
		//The initial event contains ore information than follow up events. but just extract important bits
		for (Element e : event.getProperties()) {
			AVTransportEvent avtEvent = parseEvent(e);
			currentTrack = avtEvent.getCurrentTrack();
			currentPlayMode = avtEvent.getCurrentPlayMode();
			currentPlayState = avtEvent.getTransportState();
			return;
		}
		
	}

	@Override
	public void eventReceived(UPnPEvent event) {

		for (Element e : event.getProperties()) {

			AVTransportEvent avtEvent = parseEvent(e);

			// This will always be true since we are creating
			if (!currentTrack.sameBaseTrack(avtEvent.getCurrentTrack())) {
				currentTrack = avtEvent.getCurrentTrack();
				for (SonosEventListener listener : listeners) {
					listener.trackChanged(avtEvent.getCurrentTrack());
				}
			}

			if (!currentPlayMode.equals(avtEvent.getCurrentPlayMode())) {
				currentPlayMode = avtEvent.getCurrentPlayMode();
				for (SonosEventListener listener : listeners) {
					listener.playModeChanged(avtEvent.getCurrentPlayMode());
				}
			}

			if (!currentPlayState.equals(avtEvent.getTransportState())) {
				currentPlayState = avtEvent.getTransportState();
				for (SonosEventListener listener : listeners) {
					listener.playStateChanged(avtEvent.getTransportState());
				}
			}

			for (SonosEventListener listener : listeners) {
				listener.avtTransportEvent(avtEvent);
			}
		}
	}
	
	private AVTransportEvent parseEvent(Element e) {
		Element avtEvent = ParserHelper.unwrapSonosEvent(e, upnpAVTNamespace);

		PlayState transportState = PlayState.valueOf(ParserHelper.extractEventValue(avtEvent, "TransportState",upnpAVTNamespace));
		PlayMode currentPlayModeInfo = PlayMode.valueOf(ParserHelper.extractEventValue(avtEvent, "CurrentPlayMode",upnpAVTNamespace));
		boolean crossFade = Integer.parseInt(ParserHelper.extractEventValue(avtEvent, "CurrentCrossfadeMode",upnpAVTNamespace))!= 0;
		int numberOfTracks = Integer.parseInt(ParserHelper.extractEventValue(avtEvent, "NumberOfTracks",upnpAVTNamespace));
		int currentTrackNumber = Integer.parseInt(ParserHelper.extractEventValue(avtEvent, "CurrentTrack",upnpAVTNamespace));
		int currentSection = Integer.parseInt(ParserHelper.extractEventValue(avtEvent, "CurrentSection",upnpAVTNamespace));
		String currentTrackURI = StringEscapeUtils.unescapeXml(ParserHelper.extractEventValue(avtEvent, "CurrentTrackURI",upnpAVTNamespace));
		String currentTrackDurationRaw = ParserHelper.extractEventValue(avtEvent, "CurrentTrackDuration",upnpAVTNamespace);

		int trackDurationInSeconds;
		try {
			trackDurationInSeconds = ParserHelper.formatedTimestampToSeconds(currentTrackDurationRaw);
		} catch (NumberFormatException nfe) {
			trackDurationInSeconds = 0;
		}



		TrackMetadata trackMeta = TrackMetadata
				.parse(StringEscapeUtils.unescapeXml(ParserHelper.extractEventValue(avtEvent, "CurrentTrackMetaData",upnpAVTNamespace)));

		// Care namespace!!
		String nextTrackURI = StringEscapeUtils.unescapeXml(ParserHelper.extractEventValue(avtEvent, "NextTrackURI",upnpRinnconnectsNamespace));
		TrackMetadata nextTrackMetaData = TrackMetadata
				.parse(StringEscapeUtils.unescapeXml(ParserHelper.extractEventValue(avtEvent, "NextTrackMetaData",upnpRinnconnectsNamespace)));

		// TODO what happens if we have no next track?

		TrackInfo currentTrackInfo = new TrackInfo(currentTrackNumber, trackDurationInSeconds, -1, currentTrackURI,
				trackMeta);
		TrackInfo nextTrack = new TrackInfo(currentTrackNumber + 1, -1, -1, nextTrackURI, nextTrackMetaData);

		String enqueuedTransportURI = StringEscapeUtils
				.unescapeXml(ParserHelper.extractEventValue(avtEvent, "EnqueuedTransportURI",upnpRinnconnectsNamespace));
		TrackMetadata enqueuedTransportURIMetaData = TrackMetadata.parse(
				StringEscapeUtils.unescapeXml(ParserHelper.extractEventValue(avtEvent, "EnqueuedTransportURIMetaData",upnpRinnconnectsNamespace)));

		return new AVTransportEvent(transportState, currentPlayModeInfo, crossFade,
				numberOfTracks, currentSection, currentTrackInfo, nextTrack, enqueuedTransportURI,
				enqueuedTransportURIMetaData);
		// Specialized events
	}
}
