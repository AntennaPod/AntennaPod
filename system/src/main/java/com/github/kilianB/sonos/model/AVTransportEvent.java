package com.github.kilianB.sonos.model;

/**
 * Fully parsed UPnP Transport event
 * @author Kilian
 *
 */
public class AVTransportEvent {

	
	private PlayState transportState;
	private PlayMode currentPlayMode;
	private boolean crossFade;
	private int numberOfTracks;
	private int currentSection; 
	private TrackInfo currentTrack;
	private TrackInfo nextTrack;
	private String enqueuedTransportURI;
	private TrackMetadata enqueuedTransportURIMetaData;
	
	public AVTransportEvent(PlayState transportState, PlayMode currentPlayMode, boolean crossFade, int numberOfTracks,
			int currentSection, TrackInfo currentTrack, TrackInfo nextTrack, String enqueuedTransportURI,
			TrackMetadata enqueuedTransportURIMetaData) {
		super();
		this.transportState = transportState;
		this.currentPlayMode = currentPlayMode;
		this.crossFade = crossFade;
		this.numberOfTracks = numberOfTracks;
		this.currentSection = currentSection;
		this.currentTrack = currentTrack;
		this.nextTrack = nextTrack;
		this.enqueuedTransportURI = enqueuedTransportURI;
		this.enqueuedTransportURIMetaData = enqueuedTransportURIMetaData;
	}

	public PlayState getTransportState() {
		return transportState;
	}

	public void setTransportState(PlayState transportState) {
		this.transportState = transportState;
	}

	public PlayMode getCurrentPlayMode() {
		return currentPlayMode;
	}

	public void setCurrentPlayMode(PlayMode currentPlayMode) {
		this.currentPlayMode = currentPlayMode;
	}

	public boolean isCrossFade() {
		return crossFade;
	}

	public void setCrossFade(boolean crossFade) {
		this.crossFade = crossFade;
	}

	public int getNumberOfTracks() {
		return numberOfTracks;
	}

	public void setNumberOfTracks(int numberOfTracks) {
		this.numberOfTracks = numberOfTracks;
	}

	public int getCurrentSection() {
		return currentSection;
	}

	public void setCurrentSection(int currentSection) {
		this.currentSection = currentSection;
	}

	public TrackInfo getCurrentTrack() {
		return currentTrack;
	}

	public void setCurrentTrack(TrackInfo currentTrack) {
		this.currentTrack = currentTrack;
	}

	public TrackInfo getNextTrack() {
		return nextTrack;
	}

	public void setNextTrack(TrackInfo nextTrack) {
		this.nextTrack = nextTrack;
	}

	public String getEnqueuedTransportURI() {
		return enqueuedTransportURI;
	}

	public void setEnqueuedTransportURI(String enqueuedTransportURI) {
		this.enqueuedTransportURI = enqueuedTransportURI;
	}

	public TrackMetadata getEnqueuedTransportURIMetaData() {
		return enqueuedTransportURIMetaData;
	}

	public void setEnqueuedTransportURIMetaData(TrackMetadata enqueuedTransportURIMetaData) {
		this.enqueuedTransportURIMetaData = enqueuedTransportURIMetaData;
	}

	@Override
	public String toString() {
		return "AVTransportEvent [transportState=" + transportState + ", currentPlayMode=" + currentPlayMode
				+ ", crossFade=" + crossFade + ", numberOfTracks=" + numberOfTracks + ", currentSection="
				+ currentSection + ", currentTrack=" + currentTrack + ", nextTrack=" + nextTrack
				+ ", enqueuedTransportURI=" + enqueuedTransportURI + ", enqueuedTransportURIMetaData="
				+ enqueuedTransportURIMetaData + "]";
	}
	
	
}
