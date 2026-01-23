package com.wistein.myposition

import android.app.Application
import android.content.SharedPreferences
//import android.os.StrictMode
//import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.preference.PreferenceManager
import java.lang.Exception

/***********************************************************************
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
 * MyPosition.kt
 * Application Class for MyPosition3
 *
 * Created by wmstein for myposition3 on 31.12.2016.
 * Copyright (c) 2016-2026, Wilhelm Stein, Bonn, Germany.
 * Last edited in Java on 2025-02-05,
 * converted to Kotlin on 2025-02-05,
 * last edited on 2026-01-23
 */
class MyPosition : Application() {
    override fun onCreate() {
        super.onCreate()
/*
        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        //   uncomment also last lines in myposition3.build.gradle
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i(TAG, "24, StrictMode.setVmPolicy")
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }
*/
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "37, $e")
        }

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i(TAG, "41, end of onCreate()")
        }
    }
    // End of onCreate()

    companion object {
        private const val TAG = "MyPosition"
        private var prefs: SharedPreferences? = null

        @JvmStatic
        fun getPrefs(): SharedPreferences {
            return prefs!!
        }

        @JvmField
        var lat = 0.0

        @JvmField
        var lon = 0.0

        @JvmField
        var uncertainty = 0.0

        @JvmField
        var height = 0.0
    }

}
