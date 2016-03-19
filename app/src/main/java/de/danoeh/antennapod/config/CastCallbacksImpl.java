package de.danoeh.antennapod.config;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import de.danoeh.antennapod.activity.AudioplayerActivity;
import de.danoeh.antennapod.core.CastCallbacks;

public class CastCallbacksImpl implements CastCallbacks {
    @Override
    public Class<? extends Activity> getCastActivity() {
        return AudioplayerActivity.class;
    }

    @Override
    public Intent getCastActivityIntent(Context context) {
        return new Intent(context, getCastActivity());
    }
}
