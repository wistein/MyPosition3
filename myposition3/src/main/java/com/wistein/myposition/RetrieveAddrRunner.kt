package com.wistein.myposition

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wistein.myposition.MyPositionActivity.addressLines
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/***************************************************************************************
 * Worker to get and parse address info from Nominatim Reverse Geocoder of OpenStreetMap
 *
 * Copyright 2018-2025 wistein
 * created on 2018-03-10,
 * last modification in Java on 2023-05-30,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2025-10-26
 */
class RetrieveAddrRunner(context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {
    override fun doWork(): Result {
        val rTag = "RetrvAddrRun"
        var xmlString: String
        val url: URL

        // get parameters from calling Activity
        val urlString = inputData.getString("URL_STRING") ?: return Result.failure()

        try {
            url = URL(urlString)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.readTimeout = 10000
            urlConnection.connectTimeout = 15000
            urlConnection.requestMethod = "GET"
            urlConnection.doInput = true
            urlConnection.connect()
            val status = urlConnection.responseCode
            if (status >= 400) // Error
            {
                return Result.failure()
            }

            // get the XML from input stream
            val iStream = urlConnection.inputStream
            val reader = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuilder()
            var line: String?
            try {
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append('\n')
                }
            } catch (e: IOException) {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(rTag, "58, Problem converting Stream to String: $e"
                )
            } finally {
                try {
                    iStream.close()
                } catch (e: IOException) {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.e(rTag, "65, Problem closing InputStream: $e")
                }
            }
            xmlString = sb.toString()
/*
            // Log gzip-content of url
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(rTag, "72, xmlString: $xmlString"
            )
*/
            // parse the XML content
            if (xmlString.contains("<addressparts>")) {
                var sstart = xmlString.indexOf("<addressparts>") + 14
                var send = xmlString.indexOf("</addressparts>")
                xmlString = xmlString.substring(sstart, send)
                val msg = java.lang.StringBuilder()

                // 1. line: building, viewpoint, hotel or guesthouse
                if (xmlString.contains("<building>")) {
                    sstart = xmlString.indexOf("<building>") + 10
                    send = xmlString.indexOf("</building>")
                    val building = xmlString.substring(sstart, send)
                    msg.append(building)
                    msg.append("\n")
                }
                if (xmlString.contains("<viewpoint>")) {
                    sstart = xmlString.indexOf("<viewpoint>") + 11
                    send = xmlString.indexOf("</viewpoint>")
                    val viewpoint = xmlString.substring(sstart, send)
                    msg.append(viewpoint)
                    msg.append("\n")
                }
                if (xmlString.contains("<hotel>")) {
                    sstart = xmlString.indexOf("<hotel>") + 7
                    send = xmlString.indexOf("</hotel>")
                    val hotel = xmlString.substring(sstart, send)
                    msg.append(hotel)
                    msg.append("\n")
                }
                if (xmlString.contains("<guest_house>")) {
                    sstart = xmlString.indexOf("<guest_house>") + 13
                    send = xmlString.indexOf("</guest_house>")
                    val guest_house = xmlString.substring(sstart, send)
                    msg.append(guest_house)
                    msg.append("\n")
                }

                if (xmlString.contains(">de<") || xmlString.contains(">fr<") || xmlString.contains(">ch<") || xmlString.contains(
                        ">at<"
                    ) || xmlString.contains(">it<")
                ) {
                    // 2. line: road or street, house-No.
                    if (xmlString.contains("<road>")) {
                        sstart = xmlString.indexOf("<road>") + 6
                        send = xmlString.indexOf("</road>")
                        val road = xmlString.substring(sstart, send)
                        msg.append(road)
                        msg.append(" ")
                    }
                    if (xmlString.contains("<street>")) {
                        sstart = xmlString.indexOf("<street>") + 8
                        send = xmlString.indexOf("</street>")
                        val street = xmlString.substring(sstart, send)
                        msg.append(street)
                        msg.append(" ")
                    }
                    if (xmlString.contains("<house_number>")) {
                        sstart = xmlString.indexOf("<house_number>") + 14
                        send = xmlString.indexOf("</house_number>")
                        val house_number = xmlString.substring(sstart, send)
                        msg.append(house_number)
                        msg.append("\n")
                    } else  // without house-No.
                    {
                        if (xmlString.contains("<road>") || xmlString.contains("<street>")) msg.append(
                            "\n"
                        )
                    }

                    // 3. line: city_district, suburb
                    if (xmlString.contains("<city_district>")) {
                        sstart = xmlString.indexOf("<city_district>") + 15
                        send = xmlString.indexOf("</city_district>")
                        val city_district = xmlString.substring(sstart, send)
                        msg.append(city_district)
                        if (xmlString.contains("<suburb>")) {
                            msg.append(" - ")
                        } else {
                            msg.append("\n")
                        }
                    }
                    if (xmlString.contains("<suburb>")) {
                        sstart = xmlString.indexOf("<suburb>") + 8
                        send = xmlString.indexOf("</suburb>")
                        val suburb = xmlString.substring(sstart, send)
                        msg.append(suburb)
                        msg.append("\n")
                    }

                    // 4. line: postcode village, town, city, county
                    if (xmlString.contains("<postcode>")) {
                        sstart = xmlString.indexOf("<postcode>") + 10
                        send = xmlString.indexOf("</postcode>")
                        val postcode = xmlString.substring(sstart, send)
                        msg.append(postcode)
                        msg.append(" ")
                    }

                    if (xmlString.contains("<village>")) {
                        sstart = xmlString.indexOf("<village>") + 9
                        send = xmlString.indexOf("</village>")
                        val village = xmlString.substring(sstart, send)
                        msg.append(village)
                        msg.append("\n")
                    }

                    if (xmlString.contains("<town>")) {
                        sstart = xmlString.indexOf("<town>") + 6
                        send = xmlString.indexOf("</town>")
                        val town = xmlString.substring(sstart, send)
                        msg.append(town)
                        msg.append("\n")
                    }

                    if (xmlString.contains("<city>")) {
                        sstart = xmlString.indexOf("<city>") + 6
                        send = xmlString.indexOf("</city>")
                        val city = xmlString.substring(sstart, send)
                        msg.append(city)
                        msg.append("\n")
                    }

                    if (xmlString.contains("<county>")) {
                        sstart = xmlString.indexOf("<county>") + 8
                        send = xmlString.indexOf("</county>")
                        val county = xmlString.substring(sstart, send)
                        msg.append(county)
                        msg.append("\n")
                    }

                    // 5. line: state, country
                    if (xmlString.contains("<state>")) {
                        sstart = xmlString.indexOf("<state>") + 7
                        send = xmlString.indexOf("</state>")
                        val state = xmlString.substring(sstart, send)
                        msg.append(state)
                        msg.append("\n")
                    }
                    if (xmlString.contains("<country>")) {
                        sstart = xmlString.indexOf("<country>") + 9
                        send = xmlString.indexOf("</country>")
                        val country = xmlString.substring(sstart, send)
                        msg.append(country)
                    }
                } else  // not at, ch, de, fr, it
                {
                    // 2. line: house, house-No., road or street
                    if (xmlString.contains("<house>")) {
                        sstart = xmlString.indexOf("<house>") + 7
                        send = xmlString.indexOf("</house>")
                        val house = xmlString.substring(sstart, send)
                        msg.append(house)
                        msg.append(" ")
                    }
                    if (xmlString.contains("<house_number>")) {
                        sstart = xmlString.indexOf("<house_number>") + 14
                        send = xmlString.indexOf("</house_number>")
                        val house_number = xmlString.substring(sstart, send)
                        msg.append(house_number)
                        msg.append(" ")
                    }
                    if (xmlString.contains("<road>")) {
                        sstart = xmlString.indexOf("<road>") + 6
                        send = xmlString.indexOf("</road>")
                        val road = xmlString.substring(sstart, send)
                        msg.append(road)
                        msg.append("\n")
                    }
                    if (xmlString.contains("<street>")) {
                        sstart = xmlString.indexOf("<street>") + 8
                        send = xmlString.indexOf("</street>")
                        val street = xmlString.substring(sstart, send)
                        msg.append(street)
                        msg.append("\n")
                    }

                    // 3. line: suburb
                    if (xmlString.contains("<suburb>")) {
                        sstart = xmlString.indexOf("<suburb>") + 8
                        send = xmlString.indexOf("</suburb>")
                        val suburb = xmlString.substring(sstart, send)
                        msg.append(suburb)
                        msg.append("\n")
                    }

                    // 4. line: village, town
                    if (xmlString.contains("<village>")) {
                        sstart = xmlString.indexOf("<village>") + 9
                        send = xmlString.indexOf("</village>")
                        val village = xmlString.substring(sstart, send)
                        msg.append(village)
                        msg.append("\n")
                    }
                    if (xmlString.contains("<town>")) {
                        sstart = xmlString.indexOf("<town>") + 6
                        send = xmlString.indexOf("</town>")
                        val town = xmlString.substring(sstart, send)
                        msg.append(town)
                        msg.append("\n")
                    }

                    // 5. line: city
                    if (xmlString.contains("<city>")) {
                        sstart = xmlString.indexOf("<city>") + 6
                        send = xmlString.indexOf("</city>")
                        val city = xmlString.substring(sstart, send)
                        msg.append(city)
                        msg.append("\n")
                    }

                    //6. line: county, state_district
                    if (xmlString.contains("<county>")) {
                        sstart = xmlString.indexOf("<county>") + 8
                        send = xmlString.indexOf("</county>")
                        val county = xmlString.substring(sstart, send)
                        msg.append(county)
                        msg.append("\n")
                    }

                    if (xmlString.contains("<state_district>")) {
                        sstart = xmlString.indexOf("<state_district>") + 16
                        send = xmlString.indexOf("</state_district>")
                        val state_district = xmlString.substring(sstart, send)
                        msg.append(state_district)
                        msg.append("\n")
                    }

                    // 7. line: state or country, postcode
                    if (xmlString.contains("<state>")) {
                        sstart = xmlString.indexOf("<state>") + 7
                        send = xmlString.indexOf("</state>")
                        val state = xmlString.substring(sstart, send)
                        msg.append(state)
                        msg.append("\n")
                    }
                    if (xmlString.contains("<country>")) {
                        sstart = xmlString.indexOf("<country>") + 9
                        send = xmlString.indexOf("</country>")
                        val country = xmlString.substring(sstart, send)
                        msg.append(country)
                        msg.append(", ")
                    }

                    if (xmlString.contains("<postcode>")) {
                        sstart = xmlString.indexOf("<postcode>") + 10
                        send = xmlString.indexOf("</postcode>")
                        val postcode = xmlString.substring(sstart, send)
                        msg.append(postcode)
                    }
                }
                addressLines = msg.toString()
            }
        } catch (e: IOException) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(rTag, "329, Problem with address handling: $e")
            addressLines = R.string.unknownAddr.toString()
        }
        return Result.success()
    }

}
