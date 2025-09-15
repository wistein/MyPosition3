package com.wistein.myposition

import android.app.Application
import android.content.SharedPreferences
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.preference.PreferenceManager
import java.lang.Exception

/**
 * Created by wmstein for myposition3 on 31.12.2016.
 * Copyright (c) 2016-2025, Wilhelm Stein, Bonn, Germany.
 * Last edited in Java on 2025-02-05,
 * converted to Kotlin on 2025-02-05,
 * last edited on 2025-07-08
 */
@Suppress("KotlinConstantConditions")
class MyPosition : Application() {
    override fun onCreate() {
        super.onCreate()

        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        if (MyDebug.DLOG) {
            Log.i(TAG, "24, StrictMode.setVmPolicy")
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }

        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        } catch (e: Exception) {
            if (MyDebug.DLOG) Log.e(TAG, "35, $e")
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
    }
}
