package de.danoeh.antennapod.core.util.nostr;

import java.util.ArrayList;
import java.util.List;

public class TlvInputParser {

    private TlvInputParser(){

    }

    /**
     * Parses data from Bech32 entities(strings) starting with 'nprofile1',
     * according to <a href="https://github.com/nostr-protocol/nips/blob/master/19.md">NIP-19</a>.
     * Code copied and converted
     * from <a href="https://github.com/vitorpamplona/amethyst/blob/main/quartz/src/main/java/com/vitorpamplona/quartz/encoders/Nip19.kt">here</a>
     *
     * @param profileData
     * @return java.util.List
     */
    public static List<String> profile(byte[] profileData){
        List<String> readableProfileData = new ArrayList<>();
        Tlv profileTlv = Tlv.parse(profileData);

        //Could return null
        String profileHex = profileTlv.firstAsHex(TlvTypes.SPECIAL.getId());
        String firstRelayHint = profileTlv.firstAsString(TlvTypes.RELAY.getId());

        //We store the items as parsed, i.e, list[0] = profileHex, list[1] = relayHint, etc
        readableProfileData.add(profileHex);
        readableProfileData.add(firstRelayHint);

        return readableProfileData;
    }
}
