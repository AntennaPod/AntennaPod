package de.danoeh.antennapod.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

/**
 * Callbacks for the Cast features on the core module.
 */
public interface CastCallbacks {

    Class<? extends Activity> getCastActivity();

    Intent getCastActivityIntent(Context context);

}
