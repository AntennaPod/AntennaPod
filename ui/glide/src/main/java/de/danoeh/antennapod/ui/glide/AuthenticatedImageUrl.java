package de.danoeh.antennapod.ui.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.danoeh.antennapod.model.feed.FeedPreferences;
import java.util.Objects;

public class AuthenticatedImageUrl {

    @Nullable
    public static Object create(@Nullable String url, @Nullable FeedPreferences prefs) {
        if (url != null && prefs != null && prefs.getUsername() != null && !prefs.getUsername().isEmpty()) {
            return new AuthenticatedImageUrl(url, prefs.getUsername(), prefs.getPassword());
        }
        return url;
    }

    @NonNull
    private final String url;
    @Nullable
    private final String username;
    @Nullable
    private final String password;

    public AuthenticatedImageUrl(@NonNull String url, @Nullable String username, @Nullable String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public boolean hasCredentials() {
        return username != null && !username.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthenticatedImageUrl)) {
            return false;
        }
        AuthenticatedImageUrl that = (AuthenticatedImageUrl) o;
        return url.equals(that.url)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, username, password);
    }
}
