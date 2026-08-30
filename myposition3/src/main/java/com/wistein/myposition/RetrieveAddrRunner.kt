package com.wistein.myposition

import android.content.Context
import android.content.Intent
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters

import com.wistein.myposition.MyPosition.Companion.addressLines

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

import javax.net.ssl.HttpsURLConnection

/***************************************************************************************
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <<a href="https://www.gnu.org/licenses/">...</a>>.
 *
 * RetrieveAddrRunner
 * Worker to get and parse address info from Nominatim Reverse Geocoder of OpenStreetMap
 *
 * Copyright (c) 2018-2026, Wilhelm Stein, Bonn, Germany.
 * created on 2018-03-10,
 * last modification in Java on 2023-05-30,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2026-08-30
 */
class RetrieveAddrRunner(context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {
    private var prefs = MyPosition.getPrefs()

    override fun doWork(): Result {
        val rTag = "RetrvAddrRun"
        var xmlString: String
        val sb = StringBuilder()

        // Get parameters from calling Activity
        val urlString = inputData.getString("URL_STRING") ?: return Result.failure()

        // locService: true set by LocationService, false set by MyPositionActivity
        val locService = inputData.getBoolean("LOC_SERVICE", false)

        // Get app version number for User-Agent (requested parameter for Nominatim service)
        val lastVersion = prefs.getString("PREFS_VERSION_KEY", "")
        val userAgent = "MyPosition3 $lastVersion"

        // Prepare request for Nominatim Reverse Geocoder of OpenStreetMap
        val url = URL(urlString)
        val urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.readTimeout = 10000
        urlConnection.connectTimeout = 15000
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("User-Agent", userAgent)
        urlConnection.doInput = true

        // Connect with Nominatim Reverse Geocoder of OpenStreetMap
        try {
            urlConnection.connect()
            val status = urlConnection.responseCode

            // Handle connection error
            if (status != HttpsURLConnection.HTTP_OK) {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(rTag, "79, Nominatim status: $status")

                urlConnection.disconnect()
                return Result.failure()
            }

            // Get the XML from input stream of Nominatim
            val iStream = urlConnection.inputStream
            val reader = BufferedReader(InputStreamReader(iStream))
            var line: String? = ""
            try {
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append('\n')
                }
            } catch (e: IOException) {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(rTag, "95, Problem converting Stream to String: $e")
            } finally {
                reader.close()
                iStream.close()
            }
        } catch (e: IOException) {
            // SocketTimeoutException without email
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(rTag, "103, Problem with internet address handling: $e")
            addressLines = R.string.unknownAddr.toString()
        } finally {
            urlConnection.disconnect()
        }

        xmlString = sb.toString()

        // Parse the XML content
        if (xmlString.contains("<addressparts>")) {
            var strStart = xmlString.indexOf("<addressparts>") + 14
            var strEnd = xmlString.indexOf("</addressparts>")
            xmlString = xmlString.substring(strStart, strEnd)
            val msg = java.lang.StringBuilder()

            // 1. line: building, viewpoint, hotel or guesthouse
            if (xmlString.contains("<building>")) {
                strStart = xmlString.indexOf("<building>") + 10
                strEnd = xmlString.indexOf("</building>")
                val building = xmlString.substring(strStart, strEnd)
                msg.append(building)
                msg.append("\n")
            }
            if (xmlString.contains("<viewpoint>")) {
                strStart = xmlString.indexOf("<viewpoint>") + 11
                strEnd = xmlString.indexOf("</viewpoint>")
                val viewpoint = xmlString.substring(strStart, strEnd)
                msg.append(viewpoint)
                msg.append("\n")
            }
            if (xmlString.contains("<hotel>")) {
                strStart = xmlString.indexOf("<hotel>") + 7
                strEnd = xmlString.indexOf("</hotel>")
                val hotel = xmlString.substring(strStart, strEnd)
                msg.append(hotel)
                msg.append("\n")
            }
            if (xmlString.contains("<guestHouse>")) {
                strStart = xmlString.indexOf("<guestHouse>") + 13
                strEnd = xmlString.indexOf("</guestHouse>")
                val guestHouse = xmlString.substring(strStart, strEnd)
                msg.append(guestHouse)
                msg.append("\n")
            }

            if (xmlString.contains(">de<") || xmlString.contains(">fr<")
                || xmlString.contains(">ch<") || xmlString.contains(">at<")
                || xmlString.contains(">it<")) {

                // 2. line: road or street, house-No.
                if (xmlString.contains("<road>")) {
                    strStart = xmlString.indexOf("<road>") + 6
                    strEnd = xmlString.indexOf("</road>")
                    val road = xmlString.substring(strStart, strEnd)
                    msg.append(road)
                    msg.append(" ")
                }
                if (xmlString.contains("<street>")) {
                    strStart = xmlString.indexOf("<street>") + 8
                    strEnd = xmlString.indexOf("</street>")
                    val street = xmlString.substring(strStart, strEnd)
                    msg.append(street)
                    msg.append(" ")
                }
                if (xmlString.contains("<houseNumber>")) {
                    strStart = xmlString.indexOf("<houseNumber>") + 14
                    strEnd = xmlString.indexOf("</houseNumber>")
                    val houseNumber = xmlString.substring(strStart, strEnd)
                    msg.append(houseNumber)
                    msg.append("\n")
                } else  // without house-No.
                {
                    if (xmlString.contains("<road>") || xmlString.contains("<street>"))
                        msg.append("\n")
                }

                // 3. line: cityDistrict, suburb
                if (xmlString.contains("<cityDistrict>")) {
                    strStart = xmlString.indexOf("<cityDistrict>") + 15
                    strEnd = xmlString.indexOf("</cityDistrict>")
                    val cityDistrict = xmlString.substring(strStart, strEnd)
                    msg.append(cityDistrict)
                    if (xmlString.contains("<suburb>")) {
                        msg.append(" - ")
                    } else {
                        msg.append("\n")
                    }
                }
                if (xmlString.contains("<suburb>")) {
                    strStart = xmlString.indexOf("<suburb>") + 8
                    strEnd = xmlString.indexOf("</suburb>")
                    val suburb = xmlString.substring(strStart, strEnd)
                    msg.append(suburb)
                    msg.append("\n")
                }

                // 4. line: zip code village, town, city, county
                if (xmlString.contains("<postcode>")) {
                    strStart = xmlString.indexOf("<postcode>") + 10
                    strEnd = xmlString.indexOf("</postcode>")
                    val postcode = xmlString.substring(strStart, strEnd)
                    msg.append(postcode)
                    msg.append(" ")
                }

                if (xmlString.contains("<village>")) {
                    strStart = xmlString.indexOf("<village>") + 9
                    strEnd = xmlString.indexOf("</village>")
                    val village = xmlString.substring(strStart, strEnd)
                    msg.append(village)
                    msg.append("\n")
                }

                if (xmlString.contains("<town>")) {
                    strStart = xmlString.indexOf("<town>") + 6
                    strEnd = xmlString.indexOf("</town>")
                    val town = xmlString.substring(strStart, strEnd)
                    msg.append(town)
                    msg.append("\n")
                }

                if (xmlString.contains("<city>")) {
                    strStart = xmlString.indexOf("<city>") + 6
                    strEnd = xmlString.indexOf("</city>")
                    val city = xmlString.substring(strStart, strEnd)
                    msg.append(city)
                    msg.append("\n")
                }

                if (xmlString.contains("<county>")) {
                    strStart = xmlString.indexOf("<county>") + 8
                    strEnd = xmlString.indexOf("</county>")
                    val county = xmlString.substring(strStart, strEnd)
                    msg.append(county)
                    msg.append("\n")
                }

                // 5. line: state, country
                if (xmlString.contains("<state>")) {
                    strStart = xmlString.indexOf("<state>") + 7
                    strEnd = xmlString.indexOf("</state>")
                    val state = xmlString.substring(strStart, strEnd)
                    msg.append(state)
                    msg.append("\n")
                }
                if (xmlString.contains("<country>")) {
                    strStart = xmlString.indexOf("<country>") + 9
                    strEnd = xmlString.indexOf("</country>")
                    val country = xmlString.substring(strStart, strEnd)
                    msg.append(country)
                }
            } else  // not at, ch, de, fr, it
            {
                // 2. line: house, house-No., road or street
                if (xmlString.contains("<house>")) {
                    strStart = xmlString.indexOf("<house>") + 7
                    strEnd = xmlString.indexOf("</house>")
                    val house = xmlString.substring(strStart, strEnd)
                    msg.append(house)
                    msg.append(" ")
                }
                if (xmlString.contains("<houseNumber>")) {
                    strStart = xmlString.indexOf("<houseNumber>") + 14
                    strEnd = xmlString.indexOf("</houseNumber>")
                    val houseNumber = xmlString.substring(strStart, strEnd)
                    msg.append(houseNumber)
                    msg.append(" ")
                }
                if (xmlString.contains("<road>")) {
                    strStart = xmlString.indexOf("<road>") + 6
                    strEnd = xmlString.indexOf("</road>")
                    val road = xmlString.substring(strStart, strEnd)
                    msg.append(road)
                    msg.append("\n")
                }
                if (xmlString.contains("<street>")) {
                    strStart = xmlString.indexOf("<street>") + 8
                    strEnd = xmlString.indexOf("</street>")
                    val street = xmlString.substring(strStart, strEnd)
                    msg.append(street)
                    msg.append("\n")
                }

                // 3. line: suburb
                if (xmlString.contains("<suburb>")) {
                    strStart = xmlString.indexOf("<suburb>") + 8
                    strEnd = xmlString.indexOf("</suburb>")
                    val suburb = xmlString.substring(strStart, strEnd)
                    msg.append(suburb)
                    msg.append("\n")
                }

                // 4. line: village, town
                if (xmlString.contains("<village>")) {
                    strStart = xmlString.indexOf("<village>") + 9
                    strEnd = xmlString.indexOf("</village>")
                    val village = xmlString.substring(strStart, strEnd)
                    msg.append(village)
                    msg.append("\n")
                }
                if (xmlString.contains("<town>")) {
                    strStart = xmlString.indexOf("<town>") + 6
                    strEnd = xmlString.indexOf("</town>")
                    val town = xmlString.substring(strStart, strEnd)
                    msg.append(town)
                    msg.append("\n")
                }

                // 5. line: city
                if (xmlString.contains("<city>")) {
                    strStart = xmlString.indexOf("<city>") + 6
                    strEnd = xmlString.indexOf("</city>")
                    val city = xmlString.substring(strStart, strEnd)
                    msg.append(city)
                    msg.append("\n")
                }

                // 6. line: county, stateDistrict
                if (xmlString.contains("<county>")) {
                    strStart = xmlString.indexOf("<county>") + 8
                    strEnd = xmlString.indexOf("</county>")
                    val county = xmlString.substring(strStart, strEnd)
                    msg.append(county)
                    msg.append("\n")
                }

                if (xmlString.contains("<stateDistrict>")) {
                    strStart = xmlString.indexOf("<stateDistrict>") + 16
                    strEnd = xmlString.indexOf("</stateDistrict>")
                    val stateDistrict = xmlString.substring(strStart, strEnd)
                    msg.append(stateDistrict)
                    msg.append("\n")
                }

                // 7. line: state or country, zip code
                if (xmlString.contains("<state>")) {
                    strStart = xmlString.indexOf("<state>") + 7
                    strEnd = xmlString.indexOf("</state>")
                    val state = xmlString.substring(strStart, strEnd)
                    msg.append(state)
                    msg.append("\n")
                }
                if (xmlString.contains("<country>")) {
                    strStart = xmlString.indexOf("<country>") + 9
                    strEnd = xmlString.indexOf("</country>")
                    val country = xmlString.substring(strStart, strEnd)
                    msg.append(country)
                    msg.append(", ")
                }

                if (xmlString.contains("<postcode>")) {
                    strStart = xmlString.indexOf("<postcode>") + 10
                    strEnd = xmlString.indexOf("</postcode>")
                    val postcode = xmlString.substring(strStart, strEnd)
                    msg.append(postcode)
                }
            }
            addressLines = msg.toString()
        }

        if (locService) {
            // If called from LocationService start MyPositionActivity
            val intent = Intent(applicationContext, MyPositionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            applicationContext.startActivity(intent)
        }
        return Result.success()
    }

}
