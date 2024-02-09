package de.danoeh.antennapod.core.util.nostr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 *
 * Author: tcheeric
 * Copied from: <a href="https://github.com/tcheeric/nostr-java">this repository</a>
 *
 * <p>
 * Implementation of the Bech32 encoding.</p>
 *
 * <p>
 * See
 * <a href="https://github.com/bitcoin/bips/blob/master/bip-0350.mediawiki">BIP350</a>
 * and
 * <a href="https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki">BIP173</a>
 * for details.</p>
 */
public class Bech32 {

    /**
     * The Bech32 character set for encoding.
     */
    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    /**
     * The Bech32 character set for decoding.
     */
    private static final byte[] CHARSET_REV = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            15, -1, 10, 17, 21, 20, 26, 30, 7, 5, -1, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
            1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
            1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1
    };

    private static final int BECH32_CONST = 1;
    private static final int BECH32M_CONST = 0x2bc830a3;

    public enum Encoding {
        BECH32, BECH32M
    }

    public static class Bech32Data {

        public final Encoding encoding;
        public final String hrp;
        public final byte[] data;

        private Bech32Data(final Encoding encoding, final String hrp, final byte[] data) {
            this.encoding = encoding;
            this.hrp = hrp;
            this.data = data;
        }
    }

    public static String toBech32(Bech32Prefix hrp, byte[] hexKey) throws NostrException {
        var data = convertBits(hexKey, 8, 5, true);

        return Bech32.encode(Bech32.Encoding.BECH32, hrp.getCode(), data);
    }

    public static String toBech32(Bech32Prefix hrp, String hexKey) throws NostrException {
        var data = NostrUtil.hexToBytes(hexKey);

        return toBech32(hrp, data);
    }

    // Added by squirrel
    public static String fromBech32(String strBech32) throws NostrException {
        var data = Bech32.decode(strBech32).data;

        data = convertBits(data, 5, 8, true);

        if(data == null) {
            throw new RuntimeException("Invalid null data");
        }
        // Remove trailing bit
        data = Arrays.copyOfRange(data, 0, data.length - 1);

        return NostrUtil.bytesToHex(data);
    }

    /**
     * Encode a Bech32 string.
     *
     * @param bech32
     * @return
     * @throws NostrException
     */
    public static String encode(final Bech32Data bech32) throws NostrException {
        return encode(bech32.encoding, bech32.hrp, bech32.data);
    }

    /**
     * Encode a Bech32 string.
     *
     * @param encoding
     * @param hrp
     * @param values
     * @return
     * @throws NostrException
     */
    // Modified to throw NostrExceptions
    public static String encode(Encoding encoding, String hrp, final byte[] values) throws NostrException {
        if (hrp.isEmpty()) {
            throw new NostrException("Human-readable part is too short");
        }

        hrp = hrp.toLowerCase(Locale.ROOT);
        byte[] checksum = createChecksum(encoding, hrp, values);
        byte[] combined = new byte[values.length + checksum.length];
        System.arraycopy(values, 0, combined, 0, values.length);
        System.arraycopy(checksum, 0, combined, values.length, checksum.length);
        StringBuilder sb = new StringBuilder(hrp.length() + 1 + combined.length);
        sb.append(hrp);
        sb.append('1');
        for (byte b : combined) {
            sb.append(CHARSET.charAt(b));
        }
        return sb.toString();
    }

    /**
     * Decode a Bech32 string.
     *
     * @param str
     * @return
     * @throws NostrException
     */
    // Modified to throw NostrExceptions
    public static Bech32Data decode(final String str) throws NostrException {
        boolean lower = false, upper = false;
        if (str.length() < 8) {
            throw new NostrException("Input too short: " + str.length());
        }
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < 33 || c > 126) {
                throw new NostrException(String.format("Invalid Character %c, %d", c, i));
            }
            if (c >= 'a' && c <= 'z') {
                if (upper) {
                    throw new NostrException(String.format("Invalid Character %c, %d", c, i));
                }
                lower = true;

            }
            if (c >= 'A' && c <= 'Z') {
                if (lower) {
                    throw new NostrException(String.format("Invalid Character %c, %d", c, i));
                }
                upper = true;
            }
        }
        final int pos = str.lastIndexOf('1');
        if (pos < 1) {
            throw new NostrException("Missing human-readable part");
        }
        final int dataPartLength = str.length() - 1 - pos;
        if (dataPartLength < 6) {
            throw new NostrException(String.format("Data part too short: %d)", dataPartLength));
        }
        byte[] values = new byte[dataPartLength];
        for (int i = 0; i < dataPartLength; ++i) {
            char c = str.charAt(i + pos + 1);
            if (CHARSET_REV[c] == -1) {
                throw new NostrException(String.format("Invalid Character %c, %d", c, i + pos + 1));
            }
            values[i] = CHARSET_REV[c];
        }
        String hrp = str.substring(0, pos).toLowerCase(Locale.ROOT);
        Encoding encoding = verifyChecksum(hrp, values);
        if (encoding == null) {
            throw new NostrException("InvalidChecksum");
        }
        return new Bech32Data(encoding, hrp, Arrays.copyOfRange(values, 0, values.length - 6));
    }

    /**
     * Find the polynomial with value coefficients mod the generator as 30-bit.
     */
    private static int polymod(final byte[] values) {
        int c = 1;
        for (byte v_i : values) {
            int c0 = (c >>> 25) & 0xff;
            c = ((c & 0x1ffffff) << 5) ^ (v_i & 0xff);
            if ((c0 & 1) != 0) {
                c ^= 0x3b6a57b2;
            }
            if ((c0 & 2) != 0) {
                c ^= 0x26508e6d;
            }
            if ((c0 & 4) != 0) {
                c ^= 0x1ea119fa;
            }
            if ((c0 & 8) != 0) {
                c ^= 0x3d4233dd;
            }
            if ((c0 & 16) != 0) {
                c ^= 0x2a1462b3;
            }
        }
        return c;
    }

    /**
     * Expand a HRP for use in checksum computation.
     */
    private static byte[] expandHrp(final String hrp) {
        int hrpLength = hrp.length();
        byte[] ret = new byte[hrpLength * 2 + 1];
        for (int i = 0; i < hrpLength; ++i) {
            int c = hrp.charAt(i) & 0x7f; // Limit to standard 7-bit ASCII
            ret[i] = (byte) ((c >>> 5) & 0x07);
            ret[i + hrpLength + 1] = (byte) (c & 0x1f);
        }
        ret[hrpLength] = 0;
        return ret;
    }

    /**
     * Verify a checksum.
     */
    private static Encoding verifyChecksum(final String hrp, final byte[] values) {
        byte[] hrpExpanded = expandHrp(hrp);
        byte[] combined = new byte[hrpExpanded.length + values.length];
        System.arraycopy(hrpExpanded, 0, combined, 0, hrpExpanded.length);
        System.arraycopy(values, 0, combined, hrpExpanded.length, values.length);
        final int check = polymod(combined);
        if(check == BECH32_CONST) {
           return Encoding.BECH32;
        } else if (check == BECH32M_CONST) {
           return Encoding.BECH32M;
        } else {
           return null;
        }
    }

    /**
     * Create a checksum.
     */
    private static byte[] createChecksum(final Encoding encoding, final String hrp, final byte[] values) {
        byte[] hrpExpanded = expandHrp(hrp);
        byte[] enc = new byte[hrpExpanded.length + values.length + 6];
        System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.length);
        System.arraycopy(values, 0, enc, hrpExpanded.length, values.length);
        int mod = polymod(enc) ^ (encoding == Encoding.BECH32 ? BECH32_CONST : BECH32M_CONST);
        byte[] ret = new byte[6];
        for (int i = 0; i < 6; ++i) {
            ret[i] = (byte) ((mod >>> (5 * (5 - i))) & 31);
        }
        return ret;
    }

    // Added by squirrel
    private static byte[] convertBits(byte[] data, int fromWidth, int toWidth, boolean pad) {
        int acc = 0;
        int bits = 0;
        List<Byte> result = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            int value = (data[i] & 0xff) & ((1 << fromWidth) - 1);
            acc = (acc << fromWidth) | value;
            bits += fromWidth;
            while (bits >= toWidth) {
                bits -= toWidth;
                result.add((byte) ((acc >> bits) & ((1 << toWidth) - 1)));
            }
        }
        if (pad) {
            if (bits > 0) {
                result.add((byte) ((acc << (toWidth - bits)) & ((1 << toWidth) - 1)));
            }
        } else if (bits == fromWidth || ((acc << (toWidth - bits)) & ((1 << toWidth) - 1)) != 0) {
            return null;
        }
        byte[] output = new byte[result.size()];
        for (int i = 0; i < output.length; i++) {
            output[i] = result.get(i);
        }
        return output;
    }
}
