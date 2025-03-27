package com.wistein.myposition

import kotlin.math.ceil

/********************************************************************************
 * This file uses an extract of MapUtils.java (c) OsmAnd developers, GPLv3
 * https://github.com/osmandapp/Osmand/blob/9bb03894a57cc80c2f9ad935ba007d2c406abd2c/OsmAnd-java/src/net/osmand/util/MapUtils.java
 *
 * This extract of MapUtils class from OsmAnd includes:
 * - Method to create a short link string (createShortLinkString)
 * - Subroutine to interleave the bits of 2 numbers (interleaveBits)
 *
 * Adopted 2019 by wistein for MyPosition3,
 * last edited in Java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2025-02-21.
 */
internal object MapUtils {
    /**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Base64 Alphabet" equivalents as specified
     * in Table 1 of RFC 2045.
     */
    private val intToBase64 = charArrayOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '~'
    )

    @JvmStatic
    fun createShortLinkString(latitude: Double, longitude: Double, zoom: Int): String {
        val lat = (((latitude + 90.0) / 180.0) * (1L shl 32)).toLong()
        val lon = (((longitude + 180.0) / 360.0) * (1L shl 32)).toLong()
        val code = interleaveBits(lon, lat)
        val str = StringBuilder()

        // add eight to the zoom level, which approximates an accuracy of one pixel in a tile.
        var i = 0
        while (i < ceil((zoom + 8) / 3.0)) {
            str.append(intToBase64[((code shr (58 - 6 * i)) and 0x3fL).toInt()])
            i++
        }

        // append characters onto the end of the string to represent partial zoom
        //   levels (characters themselves have a granularity of 3 zoom levels).
        (0 until (zoom + 8) % 3).forEach { j ->
            str.append('-')
        }
        return str.toString()
    }

    /**
     * Interleaves the bits of two 32-bit numbers.
     * The result is known as a Morton code.
     */
    private fun interleaveBits(x: Long, y: Long): Long {
        var c: Long = 0
        for (b in 31 downTo 0) {
            c = (c shl 1) or ((x shr b.toInt()) and 1L)
            c = (c shl 1) or ((y shr b.toInt()) and 1L)
        }
        return c
    }

}
