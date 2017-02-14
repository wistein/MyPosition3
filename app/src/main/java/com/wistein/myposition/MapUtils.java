package com.wistein.myposition;

/* 
 * This file is (c) OsmAnd developers, GPLv3 
 * https://github.com/osmandapp/Osmand/blob/9bb03894a57cc80c2f9ad935ba007d2c406abd2c/OsmAnd-java/src/net/osmand/util/MapUtils.java
 */

/**
 * This MapUtils class includes:
 * Function to create a short link string 
 */
class MapUtils
{
    private static final String BASE_SHORT_OSM_URL = "https://openstreetmap.org/go/";

    /**
     * This array is a lookup table that translates 6-bit positive integer
     * index values into their "Base64 Alphabet" equivalents as specified
     * in Table 1 of RFC 2045.
     */
    private static final char intToBase64[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', '~'
    };
    
    public static String createShortLinkString(double latitude, double longitude, int zoom)
    {
        long lat = (long) (((latitude + 90d) / 180d) * (1L << 32));
        long lon = (long) (((longitude + 180d) / 360d) * (1L << 32));
        long code = interleaveBits(lon, lat);
        String str = "";
        // add eight to the zoom level, which approximates an accuracy of one pixel in a tile.
        for (int i = 0; i < Math.ceil((zoom + 8) / 3d); i++)
        {
            str += intToBase64[(int) ((code >> (58 - 6 * i)) & 0x3f)];
        }
        // append characters onto the end of the string to represent
        // partial zoom levels (characters themselves have a granularity of 3 zoom levels).
        for (int j = 0; j < (zoom + 8) % 3; j++)
        {
            str += '-';
        }
        return str;
    }

    /**
     * interleaves the bits of two 32-bit numbers. the result is known as a Morton code.
     */
    private static long interleaveBits(long x, long y)
    {
        long c = 0;
        for (byte b = 31; b >= 0; b--)
        {
            c = (c << 1) | ((x >> b) & 1);
            c = (c << 1) | ((y >> b) & 1);
        }
        return c;
    }

}


