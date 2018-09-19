package com.wistein.myposition;
/*
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Based on
 * MyLocation 1.1c for Android <mypapit@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 *
 * Info url :
 * http://code.google.com/p/mylocation/
 * http://kirostudio.com
 * http://blog.mypapit.net/
 *
 * GetPosService.java
 *
 * Adopted from GetFixService.java by wistein for 
 * My Position Ver. 3
 * Copyright 2018, Wilhelm Stein, Germany
 */

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GetPosService extends Service implements LocationListener
{
    private static final String LOG_TAG = "MyLocationGetPos";
    public static boolean IS_SERVICE_RUNNING = false;
    public static final String START_FOREGROUND_ACTION = "START_FOREGROUND_ACTION";
    public static final String STOP_FOREGROUND_ACTION = "STOP_FOREGROUND_ACTION";
    public static final int NOTIFICATION_ID = 0x92388749;

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent.getAction().equals(START_FOREGROUND_ACTION))
        {
            if (MyDebug.LOG)
				Log.i(LOG_TAG, "Received Start Foreground Intent ");
			
            showNotification();

            LocationManager locationManager;
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
            criteria.setAltitudeRequired(true);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(false);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_LOW);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, (float) 0.1, this);
        }
        else if (intent.getAction().equals(STOP_FOREGROUND_ACTION))
        {
            if (MyDebug.LOG)
				Log.i(LOG_TAG, "Received Stop Foreground Intent");
			
            stopService();
        }
        return START_STICKY;
    }

    public void stopService()
    {
        LocationManager locationManager;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (MyDebug.LOG)
			Log.d(LOG_TAG, "Got location");

        stopService();
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        if (MyDebug.LOG)
			Log.d(LOG_TAG, "GPS provider disabled: " + provider);
    }

    @Override
    public void onProviderEnabled(String provider)
    {
        if (MyDebug.LOG)
			Log.d(LOG_TAG, "GPS provider enabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        if (MyDebug.LOG)
			Log.d(LOG_TAG, "GPS status changed: " + provider + "," + status);
    }

    private Notification getNotification(String text)
    {
        Intent notificationIntent = new Intent(this, MyPositionActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
            .setContentTitle(getString(R.string.note_pos))
            .setTicker(getString(R.string.note_pos))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

        return notification;
    }

    private void showNotification()
    {
        Notification notification = getNotification(getString(R.string.note_pos_sum));
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopService();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // Used only in case if services are bound (Bound Services).
        return null;
    }
}
