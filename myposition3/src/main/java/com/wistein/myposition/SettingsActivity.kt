package com.wistein.myposition

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

/***********************************************************************
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
 * SettingsActivity.java
 * Class for handling and displaying preferences
 *
 * Based on
 * MyLocation 1.1c for Android <mypapit></mypapit>@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 *
 * Adopted by wistein for MyPosition3
 * Copyright 2019, Wilhelm Stein, Germany
 * last edited on 2024-12-22
 */
@Suppress("KotlinConstantConditions")
class SettingsActivity : AppCompatActivity() {
    private var TAG = "MyPosSettingsAct"
    private var prefs = myPosition.getPrefs()
    private var screenOrientL: Boolean = false
    private var darkScreen: Boolean = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.LOG) Log.i(TAG, "48, onCreate");

        // Option for screen orientation
        screenOrientL = prefs.getBoolean("screen_Orientation", false)

        requestedOrientation = if (screenOrientL) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Option for dark screen background
        darkScreen = prefs.getBoolean("dark_Screen", false)
        if (MyDebug.LOG) Log.i(TAG, "61, darkScreen: $darkScreen");
        if (darkScreen) {
            setTheme(R.style.AppTheme_Dark)
        } else {
            setTheme(R.style.AppTheme_Light)
        }

        // Add preferences from resource (R.xml.preference);
        supportFragmentManager.beginTransaction().replace(
            android.R.id.content,
            MyPreferenceFragment()
        )
            .commit()
/*
        // Should restart the activity but has no function!
        darkScreen = prefs.getBoolean("dark_Screen", false)
        // Restart SettingsActivity to switch dark/light screen
        if (darkScreenOld != darkScreen) {
            if (MyDebug.LOG) Log.i(TAG, "84, darkScreenNew: $darkScreen");
            var sIntent = intent
            this@SettingsActivity.startActivity(sIntent)
        }
*/
        // new onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            @SuppressLint("UnsafeIntentLaunch")
            override fun handleOnBackPressed() {
                if (MyDebug.LOG) Log.i(TAG, "90, handleOnBackPressed")

                val mIntent = Intent(this@SettingsActivity, MyPositionActivity::class.java)
                mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(mIntent)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    class MyPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootkey: String?) {
            setPreferencesFromResource(R.xml.preference, rootkey)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

}
