package com.wistein.myposition

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest

import com.wistein.egm.EarthGravitationalModel
import com.wistein.myposition.MyPosition.Companion.corrHeight
import com.wistein.myposition.MyPosition.Companion.heightGPS
import com.wistein.myposition.MyPosition.Companion.heightNN
import com.wistein.myposition.MyPosition.Companion.isFirstLoc
import com.wistein.myposition.MyPosition.Companion.lat
import com.wistein.myposition.MyPosition.Companion.lon
import com.wistein.myposition.MyPosition.Companion.uncertainty
import com.wistein.myposition.Utils.fromHtml

import java.io.IOException

/***************************************************************************************
 * LocationService provides the location data: latitude, longitude, height, uncertainty.
 * It is started and ended by MyPositionActivity.
 *
 * You may adapt the constants within the companion object to your needs:
 * - MIN_DISTANCE_FOR_UPDATES_GPS: Long = 2 (m)
 * - MIN_DISTANCE_FOR_UPDATES_NET: Long = 10 (m)
 * - MIN_TIME_BW_UPDATES_GPS:      Long = 1500 (msec)
 * - MIN_TIME_BW_UPDATES_NET:      Long = 5000 (msec)
 *
 * Based on LocationSrv created by anupamchugh on 28/11/16, published under
 * [](https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking)
 * licensed under the MIT License.
 *
 * Adopted for MyPosition3 by wmstein on 2019-02-07,
 * last modification in Java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2026-05-14
 */
open class LocationService : Service, LocationListener {
    companion object {
        private const val TAG = "MyPos3, LocSrv"
        private const val MIN_DISTANCE_FOR_UPDATES_GPS: Long = 2
        private const val MIN_DISTANCE_FOR_UPDATES_NET: Long = 10
        private const val MIN_TIME_BW_UPDATES_GPS: Long = 1500
        private const val MIN_TIME_BW_UPDATES_NET: Long = 5000
    }

    private var mContext: Context? = null

    // exactLocation determines whether a first GPS fix has occurred
    //   and if true there is no further need for Network provider usage
    private var exactLocation = false
    private var checkGPS: Boolean = false
    private var checkNetwork: Boolean = false
    var canGetLocation: Boolean = false

    private var location: Location? = null
    protected var locationManager: LocationManager? = null
    private var locationAttributionContext: Context? = null

    private var prefs: SharedPreferences? = null
    private var emailString = "" // mail address for OSM query
    private var showHeightMessage = false

    /** Default constructor() demanded by service declaration in AndroidManifest.xml */
    constructor() {} // Deleting it produces a compilation error

    constructor(mContext: Context?) {
        this.mContext = mContext
        getLocation()
    }

    fun getLocation() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "90, getLocation()")
        locationAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("locationCheck")
            else mContext

        prefs = MyPosition.getPrefs()
        emailString = prefs!!.getString("email_String", "").toString()
        showHeightMessage = prefs!!.getBoolean("show_Toast", false)

        try {
            locationManager = locationAttributionContext!!.getSystemService(LOCATION_SERVICE) as LocationManager

            // get GPS status
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // get network provider status
            checkNetwork = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (checkGPS || checkNetwork) {
                this.canGetLocation = true
            } else {
                val mesg = getString(R.string.no_provider)
                Toast.makeText(
                    locationAttributionContext!!,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // if GPS is enabled get position using GPS Service
            if (checkGPS && canGetLocation) {
                if (ActivityCompat.checkSelfPermission(
                        locationAttributionContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES_GPS,
                        MIN_DISTANCE_FOR_UPDATES_GPS.toFloat(), this
                    )

                    if (locationManager != null) {
                        location =
                            locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (location != null) {
                            lat = location!!.latitude
                            lon = location!!.longitude
                            heightGPS = location!!.altitude
                            // Write corrected height to global var heightNN
                            if (heightGPS != 0.0) correctHeight(lat, lon, heightGPS)
                            uncertainty = location!!.accuracy.toDouble()
                            exactLocation = true
                        }
                    }
                }
            }

            if (!exactLocation) {
                // if Network is enabled and still no GPS fix achieved
                if (checkNetwork && canGetLocation) {
                    if (ActivityCompat.checkSelfPermission(
                            locationAttributionContext!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES_NET,
                            MIN_DISTANCE_FOR_UPDATES_NET.toFloat(), this
                        )

                        if (locationManager != null) {
                            location =
                                locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (location != null) {
                                lat = location!!.latitude
                                lon = location!!.longitude
                                heightNN = 0.0
                                uncertainty = 500.0
                                exactLocation = false
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "179, getLocation() $e")
        }
    }

    // Correct height with geoid offset from simplified EarthGravitationalModel
    private fun correctHeight(latitude: Double, longitude: Double, gpsHeight: Double) {
        val gh = EarthGravitationalModel()
        try {
            gh.load(locationAttributionContext) // load the WGS84 correction coefficient table egm180.txt
        } catch (_: IOException) {
            // nothing
        }

        // Calculate the offset between the ellipsoid and geoid
        try {
            corrHeight = gh.heightOffset(latitude, longitude, gpsHeight)
        } catch (_: java.lang.Exception) {
            // nothing
        }

        heightNN = gpsHeight + corrHeight
    }

    // Stop location service
    fun stopListener() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "205, stopListener()")
        try {
            if (locationManager != null) {
                locationManager!!.removeUpdates(this@LocationService)
                stopSelf()
                locationManager = null
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "214, StopListener: $e")
        }
    }

    fun getLongitude() {
        if (location != null) {
            lon = location!!.longitude
        }
    }

    fun getLatitude() {
        if (location != null) {
            lat = location!!.latitude
        }
    }

    fun getAltitude() {
        if (location != null) {
            heightGPS = location!!.altitude
            // Write corrected height to global var heightNN
            if (heightGPS != 0.0) correctHeight(lat, lon, heightGPS)
        }
    }

    fun getAccuracy() {
        if (location != null) {
            uncertainty = location!!.accuracy.toDouble()
        }
    }

    fun canGetLocation(): Boolean {
        return this.canGetLocation
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {
        // Ask Nominatim service just once on app start
        if (isFirstLoc && lat != 0.0) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "256, onLocationChanged")

            isFirstLoc = false

            val mesg: String = locationAttributionContext!!.getString(R.string.newLock) // in green
            Toast.makeText( // bright green
                locationAttributionContext,
                fromHtml("<bold><font color='#008000'>$mesg</font></bold>"),
                Toast.LENGTH_SHORT
            ).show()

            // Get initial location data from Nominatim
            val urlString: String?
            if (emailString == "") {
                urlString = ("https://nominatim.openstreetmap.org/reverse?"
                        + "email=test@temp.test" + "&format=xml&lat="
                        + lat + "&lon=" + lon + "&zoom=18&addressdetails=1")
            } else {
                urlString = ("https://nominatim.openstreetmap.org/reverse?email="
                        + emailString + "&format=xml&lat="
                        + lat + "&lon=" + lon + "&zoom=18&addressdetails=1")
            }
            val retrieveAddrWorkRequest: WorkRequest =
                OneTimeWorkRequest.Builder(RetrieveAddrRunner::class.java)
                    .setInputData(
                        Data.Builder()
                            .putString("URL_STRING", urlString)
                            .putBoolean("LOC_SERVICE", true)
                            .build()
                    )
                    .build()
            WorkManager.getInstance(this).enqueue(retrieveAddrWorkRequest)
        }
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

}
