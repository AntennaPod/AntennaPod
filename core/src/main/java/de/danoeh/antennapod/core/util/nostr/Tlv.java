package de.danoeh.antennapod.core.util.nostr;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Follows the Nostr standard for Bech32 encoded entities
 * It can be read <a href="https://github.com/nostr-protocol/nips/blob/master/19.md">here</a>
 *
 *<p>Code copied, converted and trimmed down from
 * <a href="https://github.com/vitorpamplona/amethyst/blob/main/quartz/src/main/java/com/vitorpamplona/quartz/encoders/Tlv.kt">here</a>
 *
 * <p>Author: vitorpamplona
 */

public class Tlv {
    private final Map<Byte, List<byte[]>> data;

    public Tlv(Map<Byte, List<byte[]>> data) {
        this.data = data;
    }

    public List<String> asHex(byte type) {
        List<String> list = new ArrayList<>();
        for (byte[] bytes : data.get(type)) {
            try {
                list.add(NostrUtil.bytesToHex(bytes));
            } catch (IllegalArgumentException e) {
                // Ignore invalid values
            }
        }
        return list;
    }

    public List<String> asString(byte type) {
        List<String> list = new ArrayList<>();
        for (byte[] bytes : data.get(type)) {
            list.add(new String(bytes, StandardCharsets.UTF_8));
        }
        return list;
    }

    public String firstAsHex(byte type) {
        List<String> list = asHex(type);
        return list.isEmpty() ? null : list.get(0);
    }

    public String firstAsString(byte type) {
        List<String> list = asString(type);
        return list.isEmpty() ? null : list.get(0);
    }

    public static Tlv parse(byte[] data) {
        Map<Byte, List<byte[]>> map = new HashMap<>();
        int index = 0;
        while (index < data.length) {
            byte type = data[index++];
            int len = data[index++];
            byte[] value = Arrays.copyOfRange(data, index, index + len);
            index += len;
            if (!map.containsKey(type)) {
                map.put(type, new ArrayList<>());
            }
            map.get(type).add(value);
        }
        return new Tlv(map);
    }
}
