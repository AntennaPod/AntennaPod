package de.danoeh.antennapod.service.playback;

public enum PlayerStatus {
    INDETERMINATE,  // player is currently changing its state, listeners should wait until the player has left this state.
	ERROR, 
	PREPARING, 
	PAUSED, 
	PLAYING, 
	STOPPED, 
	PREPARED, 
	SEEKING,
	INITIALIZING,			// playback service is loading the Playable's metadata
	INITIALIZED	// playback service was started, data source of media player was set.
}
