package de.danoeh.antennapod.util.flattr;

import de.danoeh.antennapod.util.flattr.FlattrStatus;

public interface FlattrThing {
	public String getTitle();
	public String getPaymentLink();
	public FlattrStatus getFlattrStatus();
}
