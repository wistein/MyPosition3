package com.wistein.myposition

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import androidx.work.WorkRequest

import com.google.android.material.snackbar.Snackbar
import com.wistein.myposition.MapUtils.createShortLinkString
import com.wistein.myposition.MyPosition.Companion.getPrefs
import com.wistein.myposition.PermissionsForegroundDialogFragment.Companion.newInstance
import com.wistein.myposition.TCLifecycleHandler.Companion.isApplicationVisible
import com.wistein.myposition.Utils.fromHtml

import java.text.DecimalFormat
import java.util.Locale
import kotlin.system.exitProcess
import androidx.core.net.toUri

/***********************************************************************
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 * 
 * MyPositionActivity.java
 * Main Activity Class for MyPosition3
 * 
 * Partly based on
 * MyLocation 1.1c for Android <mypapit></mypapit>@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 * 
 * Adopted 2019 by wistein for MyPosition3
 * Copyright 2019-2026, Wilhelm Stein, Bonn, Germany.
 * 
 * Last edited in Java on 2026-06-01,
 * converted to Kotlin on 2026-07-27,
 * last edited on 2026-08-03.
 */
class MyPositionActivity

    : AppCompatActivity(), View.OnClickListener {
    private var tvDecimalCoord: TextView? = null
    private var tvDegreeCoord: TextView? = null
    var tvLocation: TextView? = null
    var tvMessage: TextView? = null

    private var shareLocation: ImageView? = null
    private var shareDecimal: ImageView? = null
    private var shareDegree: ImageView? = null
    private var shareMessage: ImageView? = null

    private var messageHeader = "" // 1st line in mail message
    // Preferences
    private var prefs = getPrefs()
    private var emailString = "" // mail address for OSM query
    private var screenOrientL = false // option for screen orientation
    private var showHeightMessage = false // option to show height info
    private var showPosition = ""

    // The option mapLocal works only after changing the default setting for Maps to an
    //   installed Mapping app. This is especially necessary when GAPPS are present.
    //   It then lets you select where to show the map, either on the local mapping app (true)
    //   or online on OpenStreetMap (false).
    private var mapLocal = false

    // Location info handling
    var locationService: LocationService? = null
    private var locServiceOn = false // Service control flag
    private var locationPermGranted = false // Foreground location permission state

    private var doubleBackToExitPressedTwice = false

    private var baseLayout: ScrollView? = null

    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "116, onCreate()")

        // Preferences
        prefs = getPrefs()
        // option for screen orientation
        screenOrientL = prefs.getBoolean("screen_Orientation", false)
        // Option for dark screen background
        showPosition = prefs.getString("show_position", "online")!!
        mapLocal = showPosition == "offline"
        showHeightMessage = prefs.getBoolean("show_Toast", false)
        // for reliable query of Nominatim service
        emailString = prefs.getString("email_String", "")!!

        messageHeader = getString(R.string.msg_text)

        requestedOrientation = if (screenOrientL) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState) // put here for setTheme(...) to work

        // Use EdgeToEdge mode for Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)  // Android 15+, SDK 35+
        {
            this.enableEdgeToEdge()
        }

        setContentView(R.layout.activity_my_location)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.baseLayout)
        ) { v: View?, windowInsets: WindowInsetsCompat? ->
            val insets = windowInsets!!.getInsets(WindowInsetsCompat.Type.systemBars())
            val mlp = v!!.layoutParams as MarginLayoutParams
            mlp.topMargin = insets.top
            mlp.bottomMargin = insets.bottom
            mlp.leftMargin = insets.left
            mlp.rightMargin = insets.right
            v.layoutParams = mlp
            WindowInsetsCompat.CONSUMED
        }

        // Part of location permissions handling:
        //   Set flag locationPermGranted from self permissions
        locationPermGranted = this.isFineLocPermGranted

        // If not yet location permission is granted query for it
        if (!locationPermGranted) {
            newInstance().show(
                supportFragmentManager,
                PermissionsForegroundDialogFragment::class.java.name
            )
        }

        // Set title and back button in ActionBar
        baseLayout = findViewById(R.id.baseLayout)
        supportActionBar?.setTitle(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // New onBackPressed logic
        // Use only if 2 or 3 button Navigation bar is present.
        if (this.navBarMode == 0 || this.navBarMode == 1) {
            val callback = this.onBackPressedCallback
            onBackPressedDispatcher.addCallback(this, callback)
        }
    }

    val navBarMode: Int
        // End of onCreate()
        get() {
            val resources = this.getResources()

            @SuppressLint("DiscouragedApi") val resourceId = resources.getIdentifier(
                "config_navBarInteractionMode",
                "integer", "android"
            )

            // iMode = 0: 3-button, = 1: 2-button, = 2: gesture
            val iMode =
                if (resourceId > 0) resources.getInteger(resourceId) else NAVIGATION_BAR_INTERACTION_MODE_THREE_BUTTON
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "201, NavBarMode = $iMode")
            return iMode
        }

    private val onBackPressedCallback: OnBackPressedCallback
        get() {
            val m1Handler = Handler(Looper.getMainLooper())
            val r1 =
                Runnable { doubleBackToExitPressedTwice = false }

            return object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackToExitPressedTwice) {
                        m1Handler.removeCallbacks(r1)
                        finish()
                        remove()
                    } else {
                        doubleBackToExitPressedTwice = true
                        showSnackbarBlue(getString(R.string.back_twice) + "\n\n")
                        m1Handler.postDelayed(r1, 2000)
                    }
                }
            }
        }

    private val isFineLocPermGranted: Boolean
        // Test for foreground location self permission
        get() = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onResume() {
        setTheme(R.style.AppTheme_Dark)
        super.onResume() // put here for setTheme(...) to work

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "239, onResume()")

        prefs = getPrefs()
        screenOrientL = prefs.getBoolean("screen_Orientation", false)
        showPosition = prefs.getString("show_position", "online")!!
        mapLocal = showPosition == "offline"
        showHeightMessage = prefs.getBoolean("show_Toast", false)
        emailString = prefs.getString("email_String", "")!!

        messageHeader = getString(R.string.msg_text)

        requestedOrientation = if (screenOrientL) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Load and show the data, set title in ActionBar
        supportActionBar?.setTitle(R.string.app_name)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        tvDecimalCoord = findViewById(R.id.tvDecimalCoord)
        tvDegreeCoord = findViewById(R.id.tvDegreeCoord)
        tvLocation = findViewById(R.id.tvLocation)
        tvMessage = findViewById(R.id.tvMessage)

        shareLocation = findViewById(R.id.shareLocation)
        shareDecimal = findViewById(R.id.shareDecimal)
        shareDegree = findViewById(R.id.shareDegree)
        shareMessage = findViewById(R.id.shareMessage)

        shareLocation!!.isClickable = true
        shareDecimal!!.isClickable = true
        shareDegree!!.isClickable = true
        shareMessage!!.isClickable = true

        shareLocation!!.setOnClickListener(this)
        shareDecimal!!.setOnClickListener(this)
        shareDegree!!.setOnClickListener(this)
        shareMessage!!.setOnClickListener(this)

        if (MyPosition.isFirstStart) {
            // This is to remind a missing email address for Nominatim Reverse Geocoder.
            //   Info about the first GPS lock is handled in LocationService onLocationChanged().
            if (emailString == "") {
                val mesg = getString(R.string.missingEmail)
                Toast.makeText(
                    this,  // orange
                    fromHtml("<font color='#ff6000'>$mesg</font>"),
                    Toast.LENGTH_SHORT).show()
            }
            MyPosition.isFirstStart = false
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "294, onResume(), locationPermGranted: $locationPermGranted")

        // Get location with permissions check
        locationPermGranted = this.isFineLocPermGranted
        if (locationPermGranted) locationDispatcher(1) // get location with data
    }
    // End of onResume()

    public override fun onStop() {
        super.onStop()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "306, onStop()")

        shareLocation!!.setOnClickListener(null)
        shareDecimal!!.setOnClickListener(null)
        shareDegree!!.setOnClickListener(null)
        shareMessage!!.setOnClickListener(null)

        baseLayout!!.invalidate()

        // Stop Services when app is finished but not yet destroyed
        if (!isApplicationVisible) {
            // Stop location service with permissions check
            locationDispatcher(2)

            // Stop RetrieveAddrRunner
            getInstance(this).cancelAllWork()

            finishAndRemoveTask()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "331, onDestroy()")

        exitProcess(0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_my_location, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var intent: Intent?
        val id = item.itemId

        if (id == android.R.id.home)  // back button in actionBar
        {
            val m1Handler = Handler(Looper.getMainLooper())
            val r1 = Runnable { doubleBackToExitPressedTwice = false }
            if (doubleBackToExitPressedTwice) {
                m1Handler.removeCallbacks(r1)
                finish()
            } else {
                doubleBackToExitPressedTwice = true
                showSnackbarBlue(getString(R.string.back_twice) + "\n\n")
                m1Handler.postDelayed(r1, 2000)
            }
        }
        if (id == R.id.menu_getpos) {
            // Get location service with permissions check
            locationDispatcher(1) // start location service

            // Re-enter MyPositionActivity to get the new position
            intent = Intent(this@MyPositionActivity, MyPositionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        when (id) {
            R.id.menu_help -> {
                intent = Intent(this@MyPositionActivity, ShowTextDialog::class.java)
                intent.putExtra("dialog", "help")
                startActivity(intent)
            }
            R.id.menu_about -> {
                intent = Intent(this@MyPositionActivity, ShowTextDialog::class.java)
                intent.putExtra("dialog", "about")
                startActivity(intent)
            }
            R.id.menu_settings -> {
                intent = Intent(this@MyPositionActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_viewmap -> {
                if (mapLocal) {
                    // When GAPPS are present MyPosition3 by default shows location online on Google Maps.
                    // Without GAPPS or after changing the default setting for Maps, MyPosition3
                    // uses a local mapping app to show the location
                    val geo = "geo:" + MyPosition.lat + "," + MyPosition.lon + "?z=17"
                    intent = Intent(Intent.ACTION_VIEW, geo.toUri())
                } else {
                    // use browser or other web app to show location in OpenStreetMap
                    val urlView = ("https://www.openstreetmap.org/?mlat="
                            + MyPosition.lat + "&mlon=" + MyPosition.lon + "#map=17/" + MyPosition.lat + "/" + MyPosition.lon)
                    intent = Intent(Intent.ACTION_VIEW, urlView.toUri())
                }

                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    val mesg = getString(R.string.t_noapp)
                    Toast.makeText(
                        this,
                        fromHtml("<font color='red'><b>$mesg</b></font>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            R.id.menu_converter -> {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG,"409, Start ConverterAct")

                intent = Intent()
                intent.setClass(this@MyPositionActivity, ConverterActivity::class.java)
                intent.putExtra("Latitude", MyPosition.lat)
                intent.putExtra("Longitude", MyPosition.lon)
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
        }

        return super.onOptionsItemSelected(item)
    }

    // End of onOptionsItemSelected()
    // Part of location permission handling
    fun locationDispatcher(locationDispatcherMode: Int) {
        if (locationPermGranted) {
            when (locationDispatcherMode) {
                1 -> {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG,"429, locationDispatcher(1)")

                    // get location with data
                    this.loc
                    this.data
                }

                2 -> {
                    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                        Log.i(TAG,"438, locationDispatcher(2)")

                    // stop location service
                    if (locServiceOn) {
                        locationService!!.stopListener() // .stopListener(this)
                        val sIntent = Intent(this, LocationService::class.java)
                        stopService(sIntent)
                        locServiceOn = false
                    }
                }
            }
        }
    }

    val loc: Unit
        // Get the location
        get() {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG,"456, getLoc()")

            if (!locServiceOn) {
                locationService = LocationService(this)
                val sIntent = Intent(this, LocationService::class.java)
                startService(sIntent)
                locServiceOn = true
            }
            if (locationService!!.canGetLocation()) {
                locationService!!.getLongitude() // -> lon
                locationService!!.getLatitude() // -> lat
                locationService!!.getAltitude() // -> heightGPS, corrHeight, heightNN
                locationService!!.getAccuracy() // -> uncertainty
            }
        }

    val data: Unit
        // Get the location data
        @SuppressLint("DefaultLocale")
        get() {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG,"477, getData()")

            val sb: StringBuilder?
            if (locationService!!.canGetLocation()) {
                val nord = getString(R.string.nord)
                val east = getString(R.string.east)
                val west = getString(R.string.west)
                val south = getString(R.string.south)
                val uncert = getString(R.string.uncert)
                val high = getString(R.string.height)

                val directionNS = if (MyPosition.lat >= 0) nord
                else south

                val directionEW = if (MyPosition.lon >= 0) east
                else west

                sb = StringBuilder()

                var tempLat = String.format("%.5f", MyPosition.lat)
                var tempLon = String.format("%.5f", MyPosition.lon)
                var tempHeight = String.format("%.1f", MyPosition.heightNN)
                var tempUncert = String.format("%.1f", MyPosition.uncertainty)

                val language =
                    Locale.getDefault().toString().substring(0, 2)

                // for "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in numbers
                if (language == "de" || language == "es" || language == "fr"
                    || language == "it" || language == "nl" || language == "pt"
                ) {
                    tempLat = tempLat.replace('.', ',')
                    tempLon = tempLon.replace('.', ',')
                    tempHeight = tempHeight.replace('.', ',')
                    tempUncert = tempUncert.replace('.', ',')

                    sb.append(tempLat).append(" ").append(directionNS).append(",   ")
                        .append(tempLon).append(" ").append(directionEW).append("\n")
                        .append(uncert).append(" ").append(tempUncert).append(" m,   ")
                        .append(high).append(" ").append(tempHeight).append(" m")
                } else {
                    sb.append(directionNS).append(" ").append(tempLat).append(",   ")
                        .append(directionEW).append(" ").append(tempLon).append("\n")
                        .append(uncert).append(" ").append(tempUncert).append(" m,   ")
                        .append(high).append(" ").append(tempHeight).append(" m")
                }
            } else {
                sb = StringBuilder(getString(R.string.posnotknown))
            }

            // Get reverse geocoding formatted string for message
            // String addressLines1;
            if (locationService!!.canGetLocation() && (MyPosition.lat != 0.0 || MyPosition.lon != 0.0)) {
                tvDecimalCoord!!.text = sb.toString()
                tvDegreeCoord!!.text = toDegree()

                // Call reverse geocoding
                val urlString = if (emailString == "") {
                    ("https://nominatim.openstreetmap.org/reverse?"
                            + "email=test@temp.test" + "&format=xml&lat="
                            + MyPosition.lat + "&lon=" + MyPosition.lon + "&zoom=18&addressdetails=1")
                } else {
                    ("https://nominatim.openstreetmap.org/reverse?email="
                            + emailString + "&format=xml&lat="
                            + MyPosition.lat + "&lon=" + MyPosition.lon + "&zoom=18&addressdetails=1")
                }

                val retrieveAddrWorkRequest: WorkRequest =
                    OneTimeWorkRequest.Builder(RetrieveAddrRunner::class.java)
                        .setInputData(
                            Data.Builder()
                                .putString("URL_STRING", urlString)
                                .putBoolean("LOC_SERVICE", false)
                                .build()
                        )
                        .build()
                getInstance(applicationContext).enqueue(retrieveAddrWorkRequest)

                // Format TextView tvMessage,
                //   delayed for getting the result of retrieveAddrWorkRequest
                val m2Handler = Handler(Looper.getMainLooper())
                val r2: Runnable = object : Runnable {
                    var addressLines1: String? = null

                    override fun run() {
                        addressLines1 =
                            "   " + MyPosition.addressLines // addressLines is set by RetrieveAddrRunner
                        addressLines1 = addressLines1!!.replace("\n", "\n   ")

                        if (MyPosition.addressLines != "") {
                            try {
                                tvLocation!!.text = MyPosition.addressLines
                                tvMessage!!.text = getMessage(messageHeader, addressLines1)
                            } catch (_: Exception) {
                                tvLocation!!.text = getString(R.string.noAddr)
                                tvMessage!!.text = getString(R.string.noAddr)
                            }
                        } else {
                            MyPosition.addressLines = getString(R.string.noAddr)
                            tvLocation!!.text = MyPosition.addressLines
                            tvMessage!!.text = MyPosition.addressLines
                        }
                    }
                }
                m2Handler.postDelayed(r2, 500)
            } else {
                MyPosition.addressLines = getString(R.string.noAddr)
                tvLocation!!.text = MyPosition.addressLines
                tvMessage!!.text = MyPosition.addressLines
            }

            if (showHeightMessage) {
                var corrtemp = String.format("%.1f",MyPosition.corrHeight)
                var gpstemp = String.format("%.1f", MyPosition.heightGPS)
                var nntemp = String.format("%.1f", MyPosition.heightNN)

                val language = Locale.getDefault().toString().substring(0, 2)

                // for "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in numbers
                if (language == "de" || language == "es" || language == "fr" || language == "it" || language == "nl" || language == "pt") {
                    corrtemp = corrtemp.replace('.', ',')
                    gpstemp = gpstemp.replace('.', ',')
                    nntemp = nntemp.replace('.', ',')
                }

                val hToast = (getString(R.string.h_nn) + " " + nntemp + " m"
                        + " \n " + getString(R.string.h_gps) + " " + gpstemp + " m"
                        + " \n " + getString(R.string.h_corr) + " " + corrtemp + " m")
                // 3 lines message to dismiss by Ok (\n to show above Navigation Bar in 2 or 3 button mode)
                showSnackbarHeight(hToast + "\n\n")
            }
        }

    // End of getData()
    // Convert to degree
    private fun toDegree(): String {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "614, toDegree()")

        val language = Locale.getDefault().toString().substring(0, 2)
        val stringb = StringBuilder()
        var convert = LatLonConvert(MyPosition.lat)

        val nord = getString(R.string.nord)
        val east = getString(R.string.east)
        val west = getString(R.string.west)
        val south = getString(R.string.south)

        val directionNS = if (MyPosition.lat >= 0) nord
        else south

        val directionEW = if (MyPosition.lon >= 0) east
        else west

        // For "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in numbers
        if (language == "de" || language == "es" || language == "fr" || language == "it" || language == "nl" || language == "pt") {
            stringb.append(DecimalFormat("#").format(convert.degree)).append("° ")
            stringb.append(DecimalFormat("#").format(convert.minute)).append("' ")

            var sectemp = DecimalFormat("#.#").format(convert.second)
            sectemp = sectemp.replace('.', ',')
            stringb.append(sectemp).append("\" ").append(directionNS).append(",  ")

            convert = LatLonConvert(MyPosition.lon)

            stringb.append(DecimalFormat("#").format(convert.degree)).append("° ")
            stringb.append(DecimalFormat("#").format(convert.minute)).append("' ")

            sectemp = DecimalFormat("#.#").format(convert.second)
            sectemp = sectemp.replace('.', ',')
            stringb.append(sectemp).append("\" ").append(directionEW)
        } else {
            stringb.append(DecimalFormat("#").format(convert.degree)).append("° ")
            stringb.append(DecimalFormat("#").format(convert.minute)).append("' ")
            stringb.append(DecimalFormat("#.#").format(convert.second)).append("\" ")
                .append(directionNS).append(",  ")

            convert = LatLonConvert(MyPosition.lon)

            stringb.append(DecimalFormat("#").format(convert.degree)).append("° ")
            stringb.append(DecimalFormat("#").format(convert.minute)).append("' ")
            stringb.append(DecimalFormat("#.#").format(convert.second)).append("\" ")
                .append(directionEW)
        }

        return stringb.toString()
    }

    // Share button clicked next to one of the text boxes
    override fun onClick(view: View) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TITLE, "My Location")
        intent.type = "text/plain"
        val viewID = view.id
        when (viewID) {
            R.id.shareLocation -> {
                intent.putExtra(
                    Intent.EXTRA_TEXT, (getString(R.string.myLoc)
                            + "\n  " + tvLocation!!.text)
                )
            }
            R.id.shareDecimal -> {
                intent.putExtra(
                    Intent.EXTRA_TEXT, (getString(R.string.myPos)
                            + "\n  " + tvDecimalCoord!!.text)
                )
            }
            R.id.shareDegree -> {
                intent.putExtra(
                    Intent.EXTRA_TEXT, (getString(R.string.myPos)
                            + "\n  " + tvDegreeCoord!!.text)
                )
            }
            R.id.shareMessage -> {
                intent.putExtra(
                    Intent.EXTRA_TEXT, tvMessage!!.text
                )
            }
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    // Show message to share
    @SuppressLint("DefaultLocale")
    private fun getMessage(messageHeader: String?, adrlines: String?): String {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "703, getMessage to share()")

        val message = StringBuilder()
        val geoLoc = applicationContext.getString(R.string.geoloc)
        val uncert = applicationContext.getString(R.string.uncert)
        val nord = applicationContext.getString(R.string.nord)
        val east = applicationContext.getString(R.string.east)
        val west = applicationContext.getString(R.string.west)
        val south = applicationContext.getString(R.string.south)
        val lati = applicationContext.getString(R.string.lati)
        val longi = applicationContext.getString(R.string.longi)
        val high = applicationContext.getString(R.string.height)

        var tempLat = String.format("%.5f", MyPosition.lat)
        var tempLon = String.format("%.5f", MyPosition.lon)
        var tempHigh = String.format("%.1f", MyPosition.heightNN)
        var tempUncert = String.format("%.1f", MyPosition.uncertainty)

        val language = Locale.getDefault().toString().substring(0, 2)

        // For "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in numbers
        if (language == "de" || language == "es" || language == "fr"
            || language == "it" || language == "nl" || language == "pt"
        ) {
            tempLat = tempLat.replace('.', ',')
            tempLon = tempLon.replace('.', ',')
            tempHigh = tempHigh.replace('.', ',')
            tempUncert = tempUncert.replace('.', ',')
        }

        if (MyPosition.lat == 0.0 && MyPosition.lon == 0.0) {
            return applicationContext.getString(R.string.posnotknown)
        }

        val directionNS = if (MyPosition.lat >= 0) nord
        else south

        val directionEW = if (MyPosition.lon >= 0) east
        else west

        message.append(messageHeader)
        message.append("\n\nhttps://openstreetmap.org/go/")
        message.append(createShortLinkString(MyPosition.lat, MyPosition.lon, 15))
        message.append("?m")
//        message.append("\n\nhttps://maps.google.com/maps?q=loc:" + lat + "," + lon + "&z=15");
//        message.append("\n\nhttps://download.osmand.net/go?lat=" + lat + "&lon=" + lon + "&z=15");
        message.append("\n\n")
        message.append(geoLoc)
        message.append("\n   ")

        message.append(lati)
        message.append(" ")
        message.append(tempLat)
        message.append("° ")
        message.append(directionNS)
        message.append("\n   ")

        message.append(longi)
        message.append(" ")
        message.append(tempLon)
        message.append("° ")
        message.append(directionEW)
        message.append("\n   ")

        message.append(high)
        message.append(" ")
        message.append(tempHigh)
        message.append(" m\n   ")

        message.append(uncert)
        message.append(" ")
        message.append(tempUncert)
        message.append(" m\n\n")
        message.append(applicationContext.getString(R.string.toshortAddr))
        message.append("\n")
        message.append(adrlines)

        return message.toString()
    }

    // End of getMessage
    private fun showSnackbarBlue(str: String) // bold cyan text
    {
        baseLayout = findViewById(R.id.baseLayout)
        val sB = Snackbar.make(baseLayout!!, str, Snackbar.LENGTH_LONG)
        val tv = sB.getView().findViewById<TextView>(R.id.snackbar_text)
        tv.gravity = Gravity.CENTER_HORIZONTAL
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(Color.CYAN)
        tv.maxLines = 3
        sB.show()
    }

    // Blue height message with button to dismiss
    fun showSnackbarHeight(str: String) {
        baseLayout = findViewById(R.id.baseLayout)
        val sB = Snackbar.make(baseLayout!!, str, Snackbar.LENGTH_INDEFINITE)
        val tv = sB.getView().findViewById<TextView>(R.id.snackbar_text)
        tv.gravity = Gravity.CENTER_HORIZONTAL
        tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(Color.CYAN)
        tv.maxLines = 5
        sB.setAction("Ok\n") { _: View? -> sB.dismiss() }
        sB.show()
    }

    companion object {
        private const val TAG = "MyPositionAct"

        // Two-button navigation (Android P navigation mode: Back, combined Home and Recent Apps)
        //   public static final int NAVIGATION_BAR_INTERACTION_MODE_TWO_BUTTON = 1;
        // Full screen gesture mode (introduced with Android Q)
        //   public static final int NAVIGATION_BAR_INTERACTION_MODE_GESTURE = 2;
        // Classic three-button navigation (Back, Home, Recent Apps)
        //   public static final int NAVIGATION_BAR_INTERACTION_MODE_THREE_BUTTON = 0;
        const val NAVIGATION_BAR_INTERACTION_MODE_THREE_BUTTON: Int = 0
    }

}
