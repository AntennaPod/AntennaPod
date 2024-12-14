package de.danoeh.antennapod.net.common

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 *
 */
class UriUtil2Test {
    @Test
    fun testUrlEncode() {
        val testUrl = "http://example.com/this is not encoded"
        val expected = "http%3A%2F%2Fexample.com%2Fthis+is+not+encoded"
        assertEquals(expected, urlEncode(testUrl))
    }

    @Test
    fun testQueryString() {
        val params = mapOf("key1" to "value1", "key2" to "value2")
        val expected = "key1=value1&key2=value2"
        assertEquals(expected, queryString(params))
    }
}
