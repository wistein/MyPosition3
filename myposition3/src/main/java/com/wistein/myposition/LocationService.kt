@file:Suppress("KotlinConstantConditions")

package com.wistein.myposition

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.wistein.myposition.MyPosition.Companion.lat
import com.wistein.myposition.MyPosition.Companion.lon
import com.wistein.myposition.MyPosition.Companion.uncertainty
import com.wistein.myposition.Utils.fromHtml

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
 * last edited on 2026-01-23
 */
class LocationService : Service, LocationListener {
    companion object {
        private const val TAG = "MyPos3, LocSrv"
        private const val MIN_DISTANCE_FOR_UPDATES_GPS: Long = 2
        private const val MIN_DISTANCE_FOR_UPDATES_NET: Long = 10
        private const val MIN_TIME_BW_UPDATES_GPS: Long = 1500
        private const val MIN_TIME_BW_UPDATES_NET: Long = 5000
    }

    var mContext: Context? = null
    var checkGPS: Boolean = false
    var checkNetwork: Boolean = false
    var canGetLocation: Boolean = false
    private var location: Location? = null

    //    private var latitude = 0.0
//    private var longitude = 0.0
    private var heightGPS = 0.0
    protected var locationManager: LocationManager? = null
    private var locationAttributionContext: Context? = null

    // exactLocation determines whether a first GPS fix has occurred
    //   and if true there is no further need for Network provider usage
    private var exactLocation = false

    /** Default constructor() demanded by service declaration in AndroidManifest.xml */
    constructor() {} // Deleting it produces a compilation error

    constructor(mContext: Context?) {
        this.mContext = mContext
        getLocation()
    }

    fun getLocation() {
        locationAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("locationCheck")
            else this

        try {
            locationManager = mContext!!.getSystemService(LOCATION_SERVICE) as LocationManager

            // get GPS status
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // get network provider status
            checkNetwork = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (checkGPS || checkNetwork) {
                this.canGetLocation = true
            } else {
                val mesg = getString(R.string.no_provider)
                Toast.makeText(
                    mContext!!,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // if GPS is enabled get position using GPS Service
            if (checkGPS && canGetLocation) {
                if (ActivityCompat.checkSelfPermission(
                        mContext!!,
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
                            mContext!!,
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
                                heightGPS = 0.0
                                uncertainty = 500.0
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "148, getLocation() $e")
        }
    }

    // Stop location service
    fun stopListener() {
        try {
            if (locationManager != null) {
                if (ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                locationManager!!.removeUpdates(this@LocationService)
                stopSelf()
                locationManager = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "172, StopListener: $e")
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

    fun getAltitude(): Double {
        if (location != null) {
            heightGPS = location!!.altitude
        }
        return heightGPS
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
        // do nothing
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

}
