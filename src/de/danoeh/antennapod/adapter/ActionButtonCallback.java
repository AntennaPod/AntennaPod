package de.danoeh.antennapod.adapter;

public interface ActionButtonCallback {
	/** Is called when the action button of a list item has been pressed. */
	abstract void onActionButtonPressed(int position);
}
