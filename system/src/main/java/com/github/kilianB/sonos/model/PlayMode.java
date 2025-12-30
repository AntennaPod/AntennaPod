package com.github.kilianB.sonos.model;

/**
 * @author vmichalak
 */
public enum PlayMode {
	/**
	 * Turns off shuffle and repeat.
	 */
	NORMAL,

	/**
	 * Turns on repeat the current title and turns off shuffle.
	 */
	REPEAT_ONE,

	/**
	 * Turns on repeat the queue and turns off shuffle.
	 */
	REPEAT_ALL,

	/**
	 * Turns on shuffle and repeat.
	 */
	SHUFFLE,

	/**
	 * Turns on shuffle and turns off repeat.
	 */
	SHUFFLE_NOREPEAT
}
