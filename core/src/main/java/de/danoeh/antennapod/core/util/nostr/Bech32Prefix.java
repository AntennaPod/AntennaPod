package de.danoeh.antennapod.core.util.nostr;

/**
 * Author: tcheeric
 * Copied from: <a href="https://github.com/tcheeric/nostr-java">this repository</a>
 */
public enum Bech32Prefix {
    NOTE("note", "note ids"),
    NPROFILE("nprofile", "nostr profile"),
    NEVENT("nevent", "nostr event");

    private final String code;
    private final String description;

    Bech32Prefix(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }
}
