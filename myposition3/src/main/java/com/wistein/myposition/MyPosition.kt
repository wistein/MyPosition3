package com.wistein.myposition

import android.app.Application
import android.content.SharedPreferences
// import android.os.StrictMode          // Used for debugging
// import android.os.StrictMode.VmPolicy // Used for debugging
import android.util.Log
import androidx.preference.PreferenceManager

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
 * last edited on 2026-05-14
 */
class MyPosition : Application() {
    override fun onCreate() {
        super.onCreate()
/*
        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        //   uncomment also last lines in myposition3.build.gradle
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i(TAG, "40, StrictMode.setVmPolicy")
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }
*/
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i(TAG, "49, onCreate()")
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // Initiate ActivityLifecycle for stopping periodic location requests
        registerActivityLifecycleCallbacks(TCLifecycleHandler())
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
        var heightGPS = 0.0

        @JvmField
        var corrHeight = 0.0

        @JvmField
        var heightNN = 0.0

        @JvmField
        var uncertainty = 0.0

        @JvmField
        var addressLines = ""

        @JvmField
        var isFirstLoc = true

        @JvmField
        var isFirstStart = true
    }

}
