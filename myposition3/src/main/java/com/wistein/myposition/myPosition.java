package com.wistein.myposition;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.util.Log;

import androidx.preference.PreferenceManager;

/**
 * Created by wmstein for myposition3 on 31.12.2016.
 * Copyright (c) 2016-2024, Wilhelm Stein, Bonn, Germany.
 * Last edited on 2024-11-19
 */
public class myPosition extends Application
{
    private static final String TAG = "myPosition";

    private static SharedPreferences prefs;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        if (MyDebug.LOG)
        {
            Log.i(TAG, "36, StrictMode.setVmPolicy");
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());
        }

        try
        {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, e.toString());
        }
    }

    public static SharedPreferences getPrefs()
    {
        return prefs;
    }

}
