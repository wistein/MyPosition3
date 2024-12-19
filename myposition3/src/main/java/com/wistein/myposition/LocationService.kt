package com.wistein.myposition

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

/***************************************************************************************
 * LocationService provides the location data: latitude, longitude, height, uncertainty.
 * It is started and ended by MyPositionActivity.
 *
 * Based on LocationSrv created by anupamchugh on 28/11/16, published under
 * [](https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking)
 * licensed under the MIT License.
 *
 * Adopted for MyPosition3 by wmstein since 2019-02-07,
 * last modification in Java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2024-09-30.
 */
class LocationService : Service, LocationListener {
    var mContext: Context? = null
    var checkGPS: Boolean = false
    var checkNetwork: Boolean = false
    var canGetLocation: Boolean = false
    private var location: Location? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private var height = 0.0
    private var uncertainty = 0.0
    private var fixTime: Long = 0
    protected var locationManager: LocationManager? = null

    // exactLocation determines whether a first GPS fix has occurred
    //   and therefore no further need for Network provider usage
    private var exactLocation = false

    // Default constructor demanded for service declaration in AndroidManifest.xml
    constructor()

    constructor(mContext: Context?) {
        this.mContext = mContext
        getLocation()
    }

    fun getLocation() {
        try {
            locationManager = mContext!!.getSystemService(LOCATION_SERVICE) as LocationManager

            // get GPS status
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // get network provider status
            checkNetwork = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (checkGPS || checkNetwork) {
                this.canGetLocation = true
            } else {
                Toast.makeText(mContext, getString(R.string.no_provider), Toast.LENGTH_SHORT).show()
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
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_FOR_UPDATES.toFloat(), this
                    )
                    if (locationManager != null) {
                        location =
                            locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (location != null) {
                            latitude = location!!.latitude
                            longitude = location!!.longitude
                            height = location!!.altitude
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
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_FOR_UPDATES.toFloat(), this
                        )

                        if (locationManager != null) {
                            location =
                                locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (location != null) {
                                latitude = location!!.latitude
                                longitude = location!!.longitude
                                height = 0.0
                                uncertainty = 500.0
                                exactLocation = false
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (MyDebug.LOG) Log.e(TAG, e.toString())
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
                locationManager = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "StopListener: $e")
        }
    }

    fun getLongitude(): Double {
        if (location != null) {
            longitude = location!!.longitude
        }
        return longitude
    }

    fun getLatitude(): Double {
        if (location != null) {
            latitude = location!!.latitude
        }
        return latitude
    }

    val altitude: Double
        get() {
            if (location != null) {
                height = location!!.altitude
            }
            return height
        }

    val accuracy: Double
        get() {
            if (location != null) {
                uncertainty = location!!.accuracy.toDouble()
            }
            return uncertainty
        }


    val time: Long
        get() {
            if (location != null) {
                fixTime = location!!.time
            }
            return fixTime
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

    companion object {
        private const val TAG = "LocationSrv"
        private const val MIN_DISTANCE_FOR_UPDATES: Long = 5 // (m)
        private const val MIN_TIME_BW_UPDATES: Long = 2000 // (msec)
    }

}
