@file:JvmName("UriUtilKt")

package de.danoeh.antennapod.net.common

import android.os.Build
import androidx.annotation.RequiresApi
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


/**
 * Builds a query string from a map of parameters.
 * @param params The parameters to encode.
 * @return The query string.
 */
fun queryString(params: Map<String, String>): String {
    return params.toList().joinToString(separator = "&") { (key, value) ->
        "${urlEncode(key)}=${urlEncode(value)}"
    }
}


/**
 * Encodes a URL using UTF-8. Implementation is chosen based on the current platform version.
 */
interface PlatformUrlEncoder {
    fun encode(input: String?): String
}

/**
 * Signature with defined charset is only available in API 33 and above.
 */
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
class StandardUrlEncoder : PlatformUrlEncoder {
    override fun encode(input: String?): String {
        return URLEncoder.encode(input, StandardCharsets.UTF_8)
    }
}

/**
 * Legacy implementation for API 32 and below - uses the default charset, which is documented as unreliable since
 * it can differ with each platform.
 */
class LegacyUrlEncoder : PlatformUrlEncoder {
    override fun encode(input: String?): String {
        return URLEncoder.encode(input)
    }
}

private val urlEncoder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        StandardUrlEncoder()
    else
        LegacyUrlEncoder()

/**
 * Encodes a URL with as much fidelity as the platform allows.
 * @param input The string to encode.
 * @return The encoded string.
 */
fun urlEncode(input: String?): String {
    return urlEncoder.encode(input)
}
