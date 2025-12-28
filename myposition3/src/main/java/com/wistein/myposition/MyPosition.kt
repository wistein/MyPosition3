package com.wistein.myposition

import android.app.Application
import android.content.SharedPreferences
//import android.os.StrictMode
//import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.preference.PreferenceManager
import java.lang.Exception

/**
 * Created by wmstein for myposition3 on 31.12.2016.
 * Copyright (c) 2016-2025, Wilhelm Stein, Bonn, Germany.
 * Last edited in Java on 2025-02-05,
 * converted to Kotlin on 2025-02-05,
 * last edited on 2025-12-28
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
