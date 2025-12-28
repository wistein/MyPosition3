@file:Suppress("KotlinConstantConditions")

package com.wistein.myposition

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wistein.myposition.MyPosition.Companion.lat
import com.wistein.myposition.MyPosition.Companion.lon
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

/**********************************************************************
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * ConverterActivity.java
 * - Converts between Decimal GPS coordinate and DD MM SS coordinate with appropriate
 * language-specific formatting.
 * - Calculates distances between 2 coordinates.
 *
 * Based on
 * MyLocation 1.1c for Android <mypapit></mypapit>@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 *
 * Adopted 2019 by wistein for MyPosition3,
 * last edited in Java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * Last edited on 2025-12-28
 */
class ConverterActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var tvDecimalLat: EditText
    private lateinit var tvDecimalLon: EditText
    private lateinit var tvDegreeLat: EditText
    private lateinit var tvMinuteLat: EditText
    private lateinit var tvSecondLat: EditText
    private lateinit var tvDegreeLon: EditText
    private lateinit var tvMinuteLon: EditText
    private lateinit var tvSecondLon: EditText
    private lateinit var tvDecimalLat1: EditText
    private lateinit var tvDecimalLon1: EditText
    private lateinit var tvDecimalLat2: EditText
    private lateinit var tvDecimalLon2: EditText

    private lateinit var tvDistRes: TextView

    private lateinit var buttonCalc1: Button
    private lateinit var buttonCalc2: Button
    private lateinit var buttonCalc3: Button

    private lateinit var convLayout: ScrollView

    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "85, onCreate()")
        val prefs = MyPosition.getPrefs()
        val screenOrientL = prefs.getBoolean("screen_Orientation", false)
        val darkScreen = prefs.getBoolean("dark_Screen", false)

        requestedOrientation = if (screenOrientL) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (darkScreen)
        {
            setTheme(R.style.AppTheme_Dark)
        }
        else
        {
            setTheme(R.style.AppTheme_Light)
        }

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_converter)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.converterLayout))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        convLayout = findViewById(R.id.converterLayout)

        supportActionBar!!.setTitle(R.string.title_activity_converter)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        tvDecimalLat = findViewById(R.id.latDecimal)
        tvDecimalLon = findViewById(R.id.lonDecimal)
        tvDegreeLat = findViewById(R.id.degreelat)
        tvMinuteLat = findViewById(R.id.minutelat)
        tvSecondLat = findViewById(R.id.secondlat)
        tvDegreeLon = findViewById(R.id.degreelon)
        tvMinuteLon = findViewById(R.id.minutelon)
        tvSecondLon = findViewById(R.id.secondlon)
        tvDecimalLat1 = findViewById(R.id.latDec1)
        tvDecimalLon1 = findViewById(R.id.lonDec1)
        tvDecimalLat2 = findViewById(R.id.latDec2)
        tvDecimalLon2 = findViewById(R.id.lonDec2)

        tvDistRes = findViewById(R.id.distRes)

        buttonCalc1 = findViewById(R.id.buttonCalc1)
        buttonCalc2 = findViewById(R.id.buttonCalc2)
        buttonCalc3 = findViewById(R.id.buttonCalc3)

        buttonCalc1.isClickable = true
        buttonCalc2.isClickable = true
        buttonCalc3.isClickable = true
        buttonCalc1.setOnClickListener(this)
        buttonCalc2.setOnClickListener(this)
        buttonCalc3.setOnClickListener(this)

        lat = intent.getDoubleExtra("Latitude", 0.0)
        lon = intent.getDoubleExtra("Longitude", 0.0)

        // fill in the current decimal coordinates in all appropriate fields
        tvDecimalLat.setText(DecimalFormat("#.#####").format(lat))
        tvDecimalLon.setText(DecimalFormat("#.#####").format(lon))
        tvDecimalLat1.setText(DecimalFormat("#.#####").format(lat))
        tvDecimalLon1.setText(DecimalFormat("#.#####").format(lon))
        tvDecimalLat2.setText(DecimalFormat("#.#####").format(lat))
        tvDecimalLon2.setText(DecimalFormat("#.#####").format(lon))
        tvDistRes.text = DecimalFormat("#.##").format(0.000000)

        // initially calculate the current coordinates in degrees
        this.toDegree()
    }
    // End of onCreate()

    // Button clicked in one of the headlines
    override fun onClick(view: View) {
        val viewID = view.id

        if (viewID == R.id.buttonCalc1) {
            this.toDegree()
        } else if (viewID == R.id.buttonCalc2) {
            this.toDecimal()
        } else if (viewID == R.id.buttonCalc3) {
            this.sDistance()
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "195, onDestroy()")

        // All following instructions don't prohibit memory leak of ConverterActivity
        buttonCalc1.setOnClickListener(null)
        buttonCalc2.setOnClickListener(null)
        buttonCalc3.setOnClickListener(null)

        convLayout.invalidate()
        tvDecimalLat.invalidate()
        tvDecimalLon.invalidate()
        tvDegreeLat.invalidate()
        tvMinuteLat.invalidate()
        tvSecondLat.invalidate()
        tvDegreeLon.invalidate()
        tvMinuteLon.invalidate()
        tvSecondLon.invalidate()
        tvDecimalLat1.invalidate()
        tvDecimalLon1.invalidate()
        tvDecimalLat2.invalidate()
        tvDecimalLon2.invalidate()

        tvDistRes.invalidate()
    }

    private fun toDegree() {
        // Switch off keyboard
        hideKeyboard(this)
        try {
            var lattemp = (tvDecimalLat.text.toString())
            var lontemp = (tvDecimalLon.text.toString())
            lattemp = lattemp.replace(',', '.')
            lontemp = lontemp.replace(',', '.')
            val latitemp = lattemp.toDouble()
            val longitemp = lontemp.toDouble()

            var convert = LatLonConvert(latitemp)

            tvDegreeLat.setText(DecimalFormat("#").format(convert.degree))
            tvMinuteLat.setText(DecimalFormat("#").format(convert.minute))
            tvSecondLat.setText(DecimalFormat("#.##").format(convert.second))

            convert = LatLonConvert(longitemp)

            tvDegreeLon.setText(DecimalFormat("#").format(convert.degree))
            tvMinuteLon.setText(DecimalFormat("#").format(convert.minute))
            tvSecondLon.setText(DecimalFormat("#.##").format(convert.second))
        } catch (_: NumberFormatException) {
            Toast.makeText(
                applicationContext, getString(R.string.degree)
                        + " " + getString(R.string.invValue), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun toDecimal() {
        // Switch off keyboard
        hideKeyboard(this)
        try {
            var deglattemp = (tvDegreeLat.text.toString())
            var minlattemp = (tvMinuteLat.text.toString())
            var seclattemp = (tvSecondLat.text.toString())


            // for correct calculation replace ',' with '.' for German locale
            deglattemp = deglattemp.replace(',', '.')
            minlattemp = minlattemp.replace(',', '.')
            seclattemp = seclattemp.replace(',', '.')

            var convert = LatLonConvert(
                deglattemp.toDouble(),
                minlattemp.toDouble(),
                seclattemp.toDouble()
            )
            var tvtemp: String? = DecimalFormat("#.#####").format(convert.decimal)
            tvDecimalLat.setText(tvtemp)

            var deglontemp = (tvDegreeLon.text.toString())
            var minlontemp = (tvMinuteLon.text.toString())
            var seclontemp = (tvSecondLon.text.toString())

            // for correct calculation replace ',' with '.' for German locale
            deglontemp = deglontemp.replace(',', '.')
            minlontemp = minlontemp.replace(',', '.')
            seclontemp = seclontemp.replace(',', '.')

            convert = LatLonConvert(
                deglontemp.toDouble(),
                minlontemp.toDouble(),
                seclontemp.toDouble()
            )

            tvtemp = DecimalFormat("#.#####").format(convert.decimal)
            tvDecimalLon.setText(tvtemp)
        } catch (_: NumberFormatException) {
            Toast.makeText(
                applicationContext, getString(R.string.decimal)
                        + " " + getString(R.string.invValue), Toast.LENGTH_LONG
            ).show()
        }
    }

    // Calculate distance
    private fun sDistance() {
        // Switch off keyboard
        hideKeyboard(this)

        var latDec1Temp = tvDecimalLat1.text.toString()
        latDec1Temp = latDec1Temp.replace(',', '.')
        var lonDec1Temp = tvDecimalLon1.text.toString()
        lonDec1Temp = lonDec1Temp.replace(',', '.')
        var latDec2Temp = tvDecimalLat2.text.toString()
        latDec2Temp = latDec2Temp.replace(',', '.')
        var lonDec2Temp = tvDecimalLon2.text.toString()
        lonDec2Temp = lonDec2Temp.replace(',', '.')

        val latiDec1 = latDec1Temp.toDouble()
        val loniDec1 = lonDec1Temp.toDouble()
        val latiDec2 = latDec2Temp.toDouble()
        val loniDec2 = lonDec2Temp.toDouble()

        val dist = FlatEarthDist.distance(latiDec1, loniDec1, latiDec2, loniDec2)
        val distRes = DecimalFormat("#.##").format(dist)
        tvDistRes.text = distRes
    }

    /***********************************************************************
     * Calculate short distance between two points in latitude and longitude
     * Uses Pythagorean method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point
     * Distance in meters
     */
    object FlatEarthDist {
        //returns distance in meters
        fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val a = (lat1 - lat2) * distPerLat(lat1)
            val b = (lon1 - lon2) * distPerLon(lat1)
            return sqrt(a * a + b * b)
        }

        private fun distPerLon(lat: Double): Double {
            return (0.0003121092 * lat.pow(4.0)
                    + 0.0101182384 * lat.pow(3.0)
                    - 17.2385140059 * lat * lat) + 5.5485277537 * lat + 111301.967182595
        }

        private fun distPerLat(lat: Double): Double {
            return (-0.000000487305676 * lat.pow(4.0)
                    - 0.0033668574 * lat.pow(3.0)
                    + 0.4601181791 * lat * lat
                    - 1.4558127346 * lat + 110579.25662316)
        }
    }

    companion object {
        const val TAG: String = "ConverterAct"

        fun hideKeyboard(activity: Activity) {
            val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

            //Find the currently focused view, so we can grab the correct window token from it.
            val view = checkNotNull(activity.currentFocus)
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}
