/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wistein.myposition;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * Created by wmstein for myposition3 on 31.12.2016.
 * Todo: Has possible StaticFieldLeak, but don't know how to get the context otherwise,
 * though program runs without showing any side effect
 */

public class myPosition extends Application
{
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public void onCreate()
    {
        super.onCreate();
        myPosition.context = getApplicationContext();
    }

    public static Context getAppContext()
    {
        return myPosition.context;
    }
    
}
