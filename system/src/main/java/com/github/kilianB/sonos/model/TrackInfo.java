package com.github.kilianB.sonos.model;

import com.github.kilianB.sonos.ParserHelper;

/**
 * 
 * @author vmichalak
 * @author Kilian
 */
public class TrackInfo {
	private final int queueIndex;
	private final int duration;
	private final int position;
	private final String uri;
	private final TrackMetadata metadata;

	public TrackInfo(int queueIndex, int duration, int position, String uri, TrackMetadata metadata) {
		this.queueIndex = queueIndex;
		this.duration = duration;
		this.position = position;
		this.uri = uri;
		this.metadata = metadata;
	}

	public int getQueueIndex() {
		return queueIndex;
	}

	/**
	 * The song lenght in seconds
	 * @return the song duration in seconds
	 */
	public int getDuration() {
		return duration;
	}

	public String getDurationAsString() {
		return ParserHelper.secondsToFormatedTimestamp(duration);
	}

	/**
	 * Return the current song position in seconds
	 * @return the position of the song in seconds
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @return	the current position of the song in the format HH:MM:SS
	 */
	public String getPositionAsString() {
		return ParserHelper.secondsToFormatedTimestamp(position);
	}

	public String getUri() {
		return uri;
	}

	public TrackMetadata getMetadata() {
		return metadata;
	}

	@Override
	public String toString() {
		return "TrackInfo{" + "queueIndex=" + queueIndex + ", duration='" + duration + '\'' + ", position='" + position
				+ '\'' + ", uri='" + uri + '\'' + ", metadata=" + metadata + '}';
	}

	/**
	 * Compare if two track infos point to the same song
	 * 
	 * This method is used instead of equals due to some fields (e.g. position) not being taken account of.
	 * 
	 * @param infoToCompareTo The trackInfo this object gets compared to
	 * @return true if both tracks point to the same track. 
	 */
	public boolean sameBaseTrack(TrackInfo infoToCompareTo) {
		return (this.uri.equals(infoToCompareTo.uri) && this.metadata.equals(infoToCompareTo.metadata));
	}
	
	/**
	 * Return true if the track info object points at a non present track.
	 * this.getCurrentTrackInfo() will return an object like this if the queue is empty.
	 * @return true if no field of the track info is set, false otherwise
	 */
	public boolean isEmpty() {
		return (this.queueIndex == 0 && this.duration == 0 && this.position == 0 && this.uri.isEmpty());
	}

}
