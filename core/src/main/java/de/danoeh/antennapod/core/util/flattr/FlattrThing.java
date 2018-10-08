package de.danoeh.antennapod.core.util.flattr;

import android.support.annotation.Nullable;

public interface FlattrThing {
	@Nullable
    String getTitle();
	String getPaymentLink();
	FlattrStatus getFlattrStatus();
}
