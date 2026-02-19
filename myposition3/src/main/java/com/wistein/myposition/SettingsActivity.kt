package com.wistein.myposition

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

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
 * SettingsActivity.java
 * Class for handling and displaying preferences
 *
 * Based on
 * MyLocation 1.1c for Android <mypapit></mypapit>@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 *
 * Adopted 2019 by wistein for MyPosition3,
 * last edited in Java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2026-02-19
 */
class SettingsActivity : AppCompatActivity() {
    private var prefs = MyPosition.getPrefs()
    private var screenOrientL: Boolean = false
    private var darkScreen: Boolean = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "47, onCreate()")

        // Option for screen orientation
        screenOrientL = prefs.getBoolean("screen_Orientation", false)

        requestedOrientation = if (screenOrientL) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Option for dark screen background
        darkScreen = prefs.getBoolean("dark_Screen", false)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "61, onCreate(), darkScreen: $darkScreen")
        if (darkScreen) {
            setTheme(R.style.AppTheme_Dark)
        } else {
            setTheme(R.style.AppTheme_Light)
        }

        // Add preferences from resource (R.xml.preference);
        supportFragmentManager.beginTransaction().replace(
            android.R.id.content,
            MyPreferenceFragment()
        ).commit()
    }
    // End of onCreate()

    override fun onStop() {
        super.onStop()
    }

    class MyPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootkey: String?) {
            setPreferencesFromResource(R.xml.preference, rootkey)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val TAG = "SettingsAct"
    }

}
