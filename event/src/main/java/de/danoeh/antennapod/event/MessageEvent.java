package de.danoeh.antennapod.event;

import android.content.Context;
import androidx.annotation.Nullable;

import androidx.core.util.Consumer;

public class MessageEvent {

    public final String message;

    @Nullable
    public final Consumer<Context> action;

    @Nullable
    public final String actionText;

    public MessageEvent(String message) {
        this(message, null, null);
    }

    public MessageEvent(String message, Consumer<Context> action, String actionText) {
        this.message = message;
        this.action = action;
        this.actionText = actionText;
    }
}
