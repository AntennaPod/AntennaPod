package de.danoeh.antennapod.ui.glide;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A Glide model that encapsulates an image URL and the credentials required to fetch it.
 */
public class AuthenticatedImageUrl {
    private final String imageUrl;
    private final String username;
    private final String password;

    public AuthenticatedImageUrl(@NonNull String imageUrl, String username, String password) {
        this.imageUrl = imageUrl;
        this.username = username;
        this.password = password;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthenticatedImageUrl that = (AuthenticatedImageUrl) o;
        return imageUrl.equals(that.imageUrl)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageUrl, username, password);
    }
}
