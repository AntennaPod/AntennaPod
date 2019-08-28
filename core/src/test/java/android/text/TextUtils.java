package android.text;

/**
 * A slim-down version of standard {@link android.text.TextUtils} to be used in unit tests.
 */
public class TextUtils {

    /**
     * Returns true if a and b are equal, including if they are both null.
     * <p><i>Note: In platform versions 1.1 and earlier, this method only worked well if
     * both the arguments were instances of String.</i></p>
     * @param a first CharSequence to check
     * @param b second CharSequence to check
     * @return true if a and b are equal
     */
    public static boolean equals(CharSequence a, CharSequence b) {
        if (a == b) return true;
        int length;
        if (a != null && b != null && (length = a.length()) == b.length()) {
            if (a instanceof String && b instanceof String) {
                return a.equals(b);
            } else {
                for (int i = 0; i < length; i++) {
                    if (a.charAt(i) != b.charAt(i)) return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the string is <code>null</code> or has zero length.
     *
     * @param str The string to be examined, can be <code>null</code>.
     * @return <code>true</code> if the string is <code>null</code> or has zero length.
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
