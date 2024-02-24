package de.danoeh.antennapod.net.sync.nostr.util;

/**
 * Author: vitorpamplona
 */

public enum TlvTypes {
    SPECIAL((byte) 0),
    RELAY((byte) 1),
    AUTHOR((byte) 2),
    KIND((byte) 3);

    private byte id;
    

    TlvTypes(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

}
