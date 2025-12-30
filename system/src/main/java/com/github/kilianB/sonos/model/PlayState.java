package com.github.kilianB.sonos.model;

/**
 * 
 * @author vmichalak
 */
public enum PlayState {
	/**
	 * Player has an error.
	 */
	ERROR,

	/**
	 * Player is stopped.
	 */
	STOPPED,

	/**
	 * Player is playing.
	 */
	PLAYING,

	/**
	 * Player is paused.
	 */
	PAUSED_PLAYBACK,

	/**
	 * Player is loading.
	 */
	TRANSITIONING
}
