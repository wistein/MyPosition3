package com.wistein.myposition;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.util.Log;

import androidx.preference.PreferenceManager;

/**
 * Created by wmstein for myposition3 on 31.12.2016.
 * Copyright (c) 2016-2024, Wilhelm Stein, Bonn, Germany.
 * Last edited on 2024-12-20
 */
public class myPosition extends Application
{
    private static final String TAG = "MyPosition3, myPosition";

    private static SharedPreferences prefs;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        if (MyDebug.LOG)
        {
            Log.i(TAG, "29, StrictMode.setVmPolicy");
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());
        }

        try
        {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        } catch (Exception e)
        {
            if (MyDebug.LOG) Log.e(TAG, "40, " + e);
        }
    }

    public static SharedPreferences getPrefs()
    {
        return prefs;
    }

}
