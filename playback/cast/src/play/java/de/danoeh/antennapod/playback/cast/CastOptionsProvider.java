package de.danoeh.antennapod.playback.cast;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.List;

@SuppressWarnings("unused")
@SuppressLint("VisibleForTests")
public class CastOptionsProvider implements OptionsProvider {
    @Override
    @NonNull
    public CastOptions getCastOptions(@NonNull Context context) {
        return new CastOptions.Builder()
            .setReceiverApplicationId("BEBC1DB1")
            .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(@NonNull Context context) {
        return null;
    }
}