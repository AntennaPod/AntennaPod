package de.danoeh.antennapod.playback.cast;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.List;

@SuppressWarnings("unused")
public class CastOptionsProvider implements OptionsProvider {
    @Override
    @NonNull
    public CastOptions getCastOptions(@NonNull Context context) {
        return new CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(@NonNull Context context) {
        return null;
    }
}