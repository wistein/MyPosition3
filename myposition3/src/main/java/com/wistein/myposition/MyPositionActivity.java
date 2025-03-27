package com.wistein.myposition;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import com.google.android.material.snackbar.Snackbar;
import com.wistein.egm.EarthGravitationalModel;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/***********************************************************************
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation
 * <p>
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * <p>
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p>
 * MyPositionActivity.java
 * Main Activity Class for MyPosition3
 * <p>
 * Based on
 * MyLocation 1.1c for Android <mypapit@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 * <p>
 * Adopted 2019 by wistein for MyPosition3
 * Copyright 2019-2025, Wilhelm Stein, Bonn, Germany
 * last edited on 2025-02-21
 */
public class MyPositionActivity
    extends AppCompatActivity
    implements OnClickListener
{
    private static final String TAG = "MyPositionAct";

    private TextView tvDecimalCoord;
    private TextView tvDegreeCoord;
    public TextView tvLocation;
    public TextView tvMessage;
    private TextView tvUpdatedTime;

    private ImageView shareLocation;
    private ImageView shareDecimal;
    private ImageView shareDegree;
    private ImageView shareMessage;

    public static String addresslines; // formatted string for Address field
    private String messageHeader = ""; // 1st line in mail message

    // Preferences
    private SharedPreferences prefs;
    private String emailString = "";   // mail address for OSM query
    private boolean screenOrientL;     // option for screen orientation
    private boolean darkScreen;        // Option for dark screen background
    private boolean showHtToast;       // option to show toast with height info
    // the option mapLocal works only without GAPPS. It lets you select where to show the map,
    //   either on a local mapping app (true) or online (false).
    // But when GAPPS are present MyPosition3 shows the location always online on Google Maps.
    private boolean mapLocal;

    /**
     * Two-button navigation (Android P navigation mode: Back, combined Home and Recent Apps)
     * public static final int NAVIGATION_BAR_INTERACTION_MODE_TWO_BUTTON = 1;

     * Full screen gesture mode (introduced with Android Q)
     * public static final int NAVIGATION_BAR_INTERACTION_MODE_GESTURE = 2;
     */
    // Classic three-button navigation (Back, Home, Recent Apps)
    public static final int NAVIGATION_BAR_INTERACTION_MODE_THREE_BUTTON = 0;

    LocationService locationService;

    // 'locationDispatcherMode' controls location permission dispatcher:
    //1 = use location service
    //2 = end location service
    private int locationDispatcherMode;

    private boolean locServiceOn = false; // Service control flag
    private boolean locationPermGranted;  // Foreground location permission state

    // Location info handling
    private double lat, lon, uncertainty;
    private double height = 0;
    private long fixTime = 0; // GPS fix time
    private final String strTime = "10"; // option to set GPS polling time, default 10 sec

    private boolean doubleBackToExitPressedTwice = false;

    private ScrollView baseLayout;

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        if (MyDebug.DLOG) Log.i(TAG, "137, onCreate");

        prefs = MyPosition.getPrefs();

        darkScreen = prefs.getBoolean("dark_Screen", false);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if (darkScreen)
        {
            setTheme(R.style.AppTheme_Dark);
        }
        else
        {
            setTheme(R.style.AppTheme_Light);
        }

        super.onCreate(savedInstanceState); // put here for setTheme(...) to work

        setContentView(R.layout.activity_my_location);
        baseLayout = findViewById(R.id.baseLayout);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);

        // Part of location permissions handling:
        //   Set flag locationPermGranted from self permissions
        locationPermGranted = isFineLocPermGranted();
        if (MyDebug.DLOG) Log.i(TAG, "171, onCreate, locationPermGranted: "
            + locationPermGranted);

        // If not yet location permission is granted prepare and query for them
        if (!locationPermGranted)
        {
            // Reset background location permission status in case it was set previously
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("has_asked_background", false);
            editor.commit();

            // Query foreground location permission first
            PermissionsForegroundDialogFragment.newInstance().show(getSupportFragmentManager(),
                PermissionsForegroundDialogFragment.class.getName());
        }

        // New onBackPressed logic
        // Use only if 2 or 3 button Navigation bar is present.
        if (getNavBarMode() == 0 || getNavBarMode() == 1)
        {
            OnBackPressedCallback callback = getOnBackPressedCallback();
            getOnBackPressedDispatcher().addCallback(this, callback);
        }
    }
    // End of onCreate()

    public int getNavBarMode() {
        Resources resources = this.getResources();

        @SuppressLint("DiscouragedApi")
        int resourceId = resources.getIdentifier("config_navBarInteractionMode",
            "integer", "android");

        // iMode = 0: 3-button, = 1: 2-button, = 2: gesture
        int iMode = resourceId > 0 ? resources.getInteger(resourceId) :
            NAVIGATION_BAR_INTERACTION_MODE_THREE_BUTTON;
        if (MyDebug.DLOG) Log.i(TAG, "206, NavBarMode = " + iMode);
        return iMode;
    }

    private OnBackPressedCallback getOnBackPressedCallback()
    {
        final Handler m1Handler = new Handler(Looper.getMainLooper());
        final Runnable r1 = () -> doubleBackToExitPressedTwice = false;

        return new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                if (doubleBackToExitPressedTwice)
                {
                    m1Handler.removeCallbacks(r1);
                    finish();
                    remove();
                }
                else
                {
                    doubleBackToExitPressedTwice = true;
                    showSnackbarBlue(getString(R.string.back_twice));
                    m1Handler.postDelayed(r1, 1500);
                }
            }
        };
    }

    // Test for foreground location self permission
    private boolean isFineLocPermGranted()
    {
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Test for background location self permission
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean isBackLocPermGranted()
    {
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint({"SourceLockedOrientationActivity", "ApplySharedPref"})
    @Override
    public void onResume()
    {
        if (MyDebug.DLOG) Log.i(TAG, "255, onResume");

        prefs = MyPosition.getPrefs();
        messageHeader = getString(R.string.msg_text);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        darkScreen = prefs.getBoolean("dark_Screen", false);
        mapLocal = prefs.getBoolean("map_Local", false);
        showHtToast = prefs.getBoolean("show_Toast", false);
        emailString = prefs.getString("email_String", "");

        if (MyDebug.DLOG) Log.i(TAG, "264, onResume, darkScreen: " + darkScreen);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if (darkScreen)
        {
            setTheme(R.style.AppTheme_Dark);
        }
        else
        {
            setTheme(R.style.AppTheme_Light);
        }

        super.onResume(); // put here for setTheme(...) to work

        // Get location self permission state
        locationPermGranted = isFineLocPermGranted();

        // Get flag 'has_asked_background'
        boolean hasAskedBackgroundLocation = prefs.getBoolean("has_asked_background", false);
        if (MyDebug.DLOG) Log.i(TAG, "290, hasAskedBackgroundLocation: "
            + hasAskedBackgroundLocation);

        // After granting the foreground location permission
        //   asked only once the permission for background location
        if (locationPermGranted && !hasAskedBackgroundLocation
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            PermissionsBackgroundDialogFragment.newInstance().show(getSupportFragmentManager(),
                PermissionsBackgroundDialogFragment.class.getName());

            // Query result of BackgroundDialog
            if (MyDebug.DLOG) Log.i(TAG, "302, isBackLocPermGranted: " + isBackLocPermGranted());

            // Store flag 'hasAskedBackground = true' in SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("has_asked_background", true);
            editor.commit();
        }

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        df.setTimeZone(TimeZone.getDefault());
        tvDecimalCoord = findViewById(R.id.tvDecimalCoord);
        tvDegreeCoord = findViewById(R.id.tvDegreeCoord);
        tvUpdatedTime = findViewById(R.id.tvUpdatedTime);
        tvLocation = findViewById(R.id.tvLocation);
        tvMessage = findViewById(R.id.tvMessage);

        shareLocation = findViewById(R.id.shareLocation);
        shareDecimal = findViewById(R.id.shareDecimal);
        shareDegree = findViewById(R.id.shareDegree);
        shareMessage = findViewById(R.id.shareMessage);

        shareLocation.setClickable(true);
        shareDecimal.setClickable(true);
        shareDegree.setClickable(true);
        shareMessage.setClickable(true);

        shareLocation.setOnClickListener(this);
        shareDecimal.setOnClickListener(this);
        shareDegree.setOnClickListener(this);
        shareMessage.setOnClickListener(this);

        if (MyDebug.DLOG) Log.i(TAG, "333, onResume, locationPermGranted: " + locationPermGranted);

        // Get location with permissions check
        locationDispatcherMode = 1; // get location
        locationDispatcher();
        registerRelativeFixTime();
    }
    // End of onResume()

    @SuppressLint("ApplySharedPref")
    @Override
    public void onPause()
    {
        super.onPause();
    }

    public void onStop()
    {
        super.onStop();

        if (MyDebug.DLOG) Log.i(TAG, "353, onStop");

        // Stop location service with permissions check
        locationDispatcherMode = 2;
        locationDispatcher();

        // Stop RetrieveAddrRunner
        WorkManager.getInstance(this).cancelAllWork();

        shareLocation.setOnClickListener(null);
        shareDecimal.setOnClickListener(null);
        shareDegree.setOnClickListener(null);
        shareMessage.setOnClickListener(null);

        baseLayout.invalidate();
    }

    public void onDestroy()
    {
        super.onDestroy();

        if (MyDebug.DLOG) Log.i(TAG, "374, onDestroy");
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_my_location, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;
        int id = item.getItemId();

        if (id == R.id.menu_getpos)
        {
            // Get location with permissions check
            locationDispatcherMode = 1; // get location

            // call DummyActivity to reenter MyPositionActivity to get the new position
            intent = new Intent(MyPositionActivity.this, DummyActivity.class);
            startActivity(intent);
        }
        if (id == R.id.menu_help)
        {
            intent = new Intent(MyPositionActivity.this, HelpDialog.class);
            startActivity(intent);
        }
        else if (id == R.id.menu_about)
        {
            intent = new Intent(MyPositionActivity.this, AboutDialog.class);
            startActivity(intent);
        }
        else if (id == R.id.menu_settings)
        {
            intent = new Intent(MyPositionActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.menu_viewmap)
        {
            if (mapLocal)
            {
                // when GAPPS are present MyPosition3 shows location online on Google Maps
                // without GAPPS MyPosition3 uses a local mapping app to show the location
                String geo = "geo:" + lat + "," + lon + "?z=17";
                intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(geo));
            }
            else
            {
                // use browser or other web app to show location in OpenStreetMap
                String urlView = "https://www.openstreetmap.org/?mlat="
                    + lat + "&mlon=" + lon + "#map=17/" + lat + "/" + lon;
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlView));
            }

            try
            {
                startActivity(intent);
            } catch (ActivityNotFoundException e)
            {
                Toast.makeText(this, getString(R.string.t_noapp), Toast.LENGTH_LONG).show();
            }
        }
        else if (id == R.id.menu_converter)
        {
            locationDispatcherMode = 2;
            locationDispatcher();
            if (MyDebug.DLOG) Log.i(TAG, "441, Start ConverterAct");

            intent = new Intent();
            intent.setClass(MyPositionActivity.this, ConverterActivity.class);
            intent.putExtra("Latitude", lat);
            intent.putExtra("Longitude", lon);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }

        return super.onOptionsItemSelected(item);
    }
    // End of onOptionsItemSelected()

    // Part of location permission handling
    public void locationDispatcher()
    {
        if (locationPermGranted)
        {
            switch (locationDispatcherMode)
            {
                case 1 ->
                {
                    // get location data
                    getLoc();
                }
                case 2 ->
                {
                    // stop location service
                    if (locServiceOn)
                    {
                        locationService.stopListener(); // .stopListener(this)
                        Intent sIntent = new Intent(this, LocationService.class);
                        stopService(sIntent);
                        locServiceOn = false;
                        if (MyDebug.DLOG) Log.i(TAG, "475, Stop locationService");
                    }
                }
            }
        }
    }

    // get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);
        Intent sIntent = new Intent(this, LocationService.class);
        startService(sIntent);
        locServiceOn = true;

        StringBuilder sb;
        if (locationService.canGetLocation())
        {
            lon = locationService.getLongitude();
            lat = locationService.getLatitude();
            height = locationService.getAltitude();
            if (height != 0)
                height = correctHeight(lat, lon, height);
            uncertainty = locationService.getAccuracy();
            fixTime = locationService.getTime();

            String nord = getString(R.string.nord);
            String east = getString(R.string.east);
            String west = getString(R.string.west);
            String south = getString(R.string.south);
            String directionNS, directionEW;
            String uncert = getString(R.string.uncert);
            String high = getString(R.string.height);

            if (lat >= 0)
                directionNS = nord;
            else
                directionNS = south;

            if (lon >= 0)
                directionEW = east;
            else
                directionEW = west;

            sb = new StringBuilder();

            @SuppressLint("DefaultLocale") String lattemp = String.format("%.5f", lat); // warnings not relevant here
            @SuppressLint("DefaultLocale") String lontemp = String.format("%.5f", lon);
            @SuppressLint("DefaultLocale") String heighttemp = String.format("%.1f", height);
            @SuppressLint("DefaultLocale") String uncerttemp = String.format("%.1f", uncertainty);

            String language = Locale.getDefault().toString().substring(0, 2);

            // for "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in mumbers
            if (language.equals("de") || language.equals("es") || language.equals("fr")
                || language.equals("it") || language.equals("nl") || language.equals("pt"))
            {
                lattemp = lattemp.replace('.', ',');
                lontemp = lontemp.replace('.', ',');
                heighttemp = heighttemp.replace('.', ',');
                uncerttemp = uncerttemp.replace('.', ',');

                sb.append(lattemp).append(" ").append(directionNS).append(",   ")
                    .append(lontemp).append(" ").append(directionEW).append("\n")
                    .append(uncert).append(" ").append(uncerttemp).append(" m,   ")
                    .append(high).append(" ").append(heighttemp).append(" m");
            }
            else
            {
                sb.append(directionNS).append(" ").append(lattemp).append(",   ")
                    .append(directionEW).append(" ").append(lontemp).append("\n")
                    .append(uncert).append(" ").append(uncerttemp).append(" m,   ")
                    .append(high).append(" ").append(heighttemp).append(" m");
            }

        }
        else
        {
            sb = new StringBuilder(getString(R.string.posnotknown));
        }

        // Get reverse geocoding formatted string for message
        // String addresslines1;
        if (locationService.canGetLocation() && (lat != 0 || lon != 0))
        {
            final String time_header = this.getString(R.string.last_fix_time) + " ";

            String relative_date = getString(R.string.unknownFix);
            if (fixTime > 0)
            {
                relative_date = DateUtils.getRelativeTimeSpanString(fixTime,
                    System.currentTimeMillis(), 0, 0).toString();
            }
            tvDecimalCoord.setText(sb.toString());
            tvDegreeCoord.setText(toDegree(lat, lon));
            String uTime = time_header + relative_date;
            tvUpdatedTime.setText(uTime);

            // call reverse geocoding
            String urlString = "https://nominatim.openstreetmap.org/reverse?email="
                + emailString + "&format=xml&lat="
                + lat + "&lon=" + lon + "&zoom=18&addressdetails=1";

            WorkRequest retrieveAddrWorkRequest =
                new OneTimeWorkRequest.Builder(RetrieveAddrRunner.class)
                    .setInputData(new Data.Builder()
                            .putString("URL_STRING", urlString)
                            .build()
                                 )
                    .build();

            WorkManager.getInstance(getApplicationContext())
                .enqueue(retrieveAddrWorkRequest);

            // format for TextView tvMessage,
            // delayed for getting the result of WorkRequest
            final Handler m2Handler = new Handler(Looper.getMainLooper());
            final Runnable r2 = new Runnable()
            {
                String addresslines1;

                @Override
                public void run()
                {
                    addresslines1 = "   " + addresslines; // addresslines is set by RetrieveAddrRunner
                    addresslines1 = addresslines1.replace("\n", "\n   ");

                    if (!Objects.equals(addresslines, ""))
                    {
                        try
                        {
                            tvLocation.setText(addresslines);
                            tvMessage.setText(getMessage(lat, lon, height,
                                uncertainty, messageHeader, addresslines1));
                        } catch (Exception e)
                        {
                            tvLocation.setText(getString(R.string.noAddr));
                            tvMessage.setText(getString(R.string.noAddr));
                        }
                    }
                    else
                    {
                        addresslines = getString(R.string.noAddr);
                        tvLocation.setText(addresslines);
                        tvMessage.setText(addresslines);
                    }
                }
            };
            m2Handler.postDelayed(r2, 500);
        }
        else
        {
            addresslines = getString(R.string.noAddr);
            tvLocation.setText(addresslines);
            tvMessage.setText(addresslines);
        }
    }
    // End of getLoc()

    private double correctHeight(double lat, double lon, double gpsHeight)
    {
        double corrHeight;
        double nnHeight;

        EarthGravitationalModel gh = new EarthGravitationalModel();
        try
        {
            gh.load(this); // load the WGS84 correction coefficient table egm180.txt
        } catch (IOException e)
        {
            return 0;
        }

        // Calculate the offset between the ellipsoid and geoid
        try
        {
            corrHeight = gh.heightOffset(lat, lon, gpsHeight);
        } catch (Exception e)
        {
            return 0;
        }

        nnHeight = gpsHeight + corrHeight;

        if (showHtToast)
        {
            @SuppressLint("DefaultLocale") String corrtemp = String.format("%.1f", corrHeight); // warnings not relevant here
            @SuppressLint("DefaultLocale") String gpstemp = String.format("%.1f", gpsHeight);
            @SuppressLint("DefaultLocale") String nntemp = String.format("%.1f", nnHeight);

            String language = Locale.getDefault().toString().substring(0, 2);
            // for "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in mumbers
            if (language.equals("de") || language.equals("es") || language.equals("fr") || language.equals("it") || language.equals("nl") || language.equals("pt"))
            {
                corrtemp = corrtemp.replace('.', ',');
                gpstemp = gpstemp.replace('.', ',');
                nntemp = nntemp.replace('.', ',');
            }

            String hToast = getString(R.string.h_nn) + " " + nntemp
                + " \n " + getString(R.string.h_gps) + " " + gpstemp
                + " \n " + getString(R.string.h_corr) + " " + corrtemp;
            Toast.makeText(this, hToast, Toast.LENGTH_LONG).show();
        }
        return nnHeight;
    }

    // Convert to degree
    private String toDegree(double lat, double lon)
    {
        String language = Locale.getDefault().toString().substring(0, 2);
        StringBuilder stringb = new StringBuilder();
        LatLonConvert convert = new LatLonConvert(lat);

        String nord = getString(R.string.nord);
        String east = getString(R.string.east);
        String west = getString(R.string.west);
        String south = getString(R.string.south);
        String directionNS, directionEW;

        if (lat >= 0)
            directionNS = nord;
        else
            directionNS = south;

        if (lon >= 0)
            directionEW = east;
        else
            directionEW = west;

        // For "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in mumbers
        if (language.equals("de") || language.equals("es") || language.equals("fr") || language.equals("it") || language.equals("nl") || language.equals("pt"))
        {
            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("° ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("' ");

            String sectemp = new DecimalFormat("#.#").format(convert.getSecond());
            sectemp = sectemp.replace('.', ',');
            stringb.append(sectemp).append("\" ").append(directionNS).append(",  ");

            convert = new LatLonConvert(lon);

            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("° ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("' ");

            sectemp = new DecimalFormat("#.#").format(convert.getSecond());
            sectemp = sectemp.replace('.', ',');
            stringb.append(sectemp).append("\" ").append(directionEW);
        }
        else
        {
            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("° ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("' ");
            stringb.append(new DecimalFormat("#.#").format(convert.getSecond())).append("\" ").append(directionNS).append(",  ");

            convert = new LatLonConvert(lon);

            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("° ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("' ");
            stringb.append(new DecimalFormat("#.#").format(convert.getSecond())).append("\" ").append(directionEW);
        }

        return stringb.toString();
    }

    // Share button clicked next to one of the text boxes
    @Override
    public void onClick(View view)
    {
        Intent intent;
        intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TITLE, "My Location");
        intent.setType("text/plain");
        int viewID = view.getId();
        if (viewID == R.id.shareLocation)
        {
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.myLoc)
                + "\n  " + tvLocation.getText());
        }
        else if (viewID == R.id.shareDecimal)
        {
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.myPos)
                + "\n  " + tvDecimalCoord.getText());
        }
        else if (viewID == R.id.shareDegree)
        {
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.myPos)
                + "\n  " + tvDegreeCoord.getText());
        }
        else if (viewID == R.id.shareMessage)
        {
            intent.putExtra(Intent.EXTRA_TEXT, tvMessage.getText());
        }
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    // Show message to share
    private String getMessage(double lat, double lon, double height, double uncertainty,
                              String messageHeader, String adrlines)
    {
        StringBuilder message = new StringBuilder();
        String geoLoc = getApplicationContext().getString(R.string.geoloc);
        String uncert = getApplicationContext().getString(R.string.uncert);
        String nord = getApplicationContext().getString(R.string.nord);
        String east = getApplicationContext().getString(R.string.east);
        String west = getApplicationContext().getString(R.string.west);
        String south = getApplicationContext().getString(R.string.south);
        String lati = getApplicationContext().getString(R.string.lati);
        String longi = getApplicationContext().getString(R.string.longi);
        String directionNS, directionEW;
        String high = getApplicationContext().getString(R.string.height);

        @SuppressLint("DefaultLocale") String tempLat = String.format("%.5f", lat); // warnings not relevant here
        @SuppressLint("DefaultLocale") String tempLon = String.format("%.5f", lon);
        @SuppressLint("DefaultLocale") String tempHigh = String.format("%.1f", height);
        @SuppressLint("DefaultLocale") String tempUncert = String.format("%.1f", uncertainty);

        String language = Locale.getDefault().toString().substring(0, 2);
        // For "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in mumbers
        if (language.equals("de") || language.equals("es") || language.equals("fr")
            || language.equals("it") || language.equals("nl") || language.equals("pt"))
        {
            tempLat = tempLat.replace('.', ',');
            tempLon = tempLon.replace('.', ',');
            tempHigh = tempHigh.replace('.', ',');
            tempUncert = tempUncert.replace('.', ',');
        }

        if (lat == 0.0 && lon == 0.0)
        {
            return getApplicationContext().getString(R.string.posnotknown);
        }

        if (lat >= 0)
            directionNS = nord;
        else
            directionNS = south;

        if (lon >= 0)
            directionEW = east;
        else
            directionEW = west;

        message.append(messageHeader);
        message.append("\n\nhttps://openstreetmap.org/go/");
        message.append(MapUtils.createShortLinkString(lat, lon, 15));
        message.append("?m");
//        message.append("\n\nhttps://maps.google.com/maps?q=loc:" + lat + "," + lon + "&z=15");
//        message.append("\n\nhttp://download.osmand.net/go?lat=" + lat + "&lon=" + lon + "&z=15");
        message.append("\n\n");
        message.append(geoLoc);
        message.append("\n   ");

        message.append(lati);
        message.append(" ");
        message.append(tempLat);
        message.append("° ");
        message.append(directionNS);
        message.append("\n   ");

        message.append(longi);
        message.append(" ");
        message.append(tempLon);
        message.append("° ");
        message.append(directionEW);
        message.append("\n   ");

        message.append(high);
        message.append(" ");
        message.append(tempHigh);
        message.append(" m\n   ");

        message.append(uncert);
        message.append(" ");
        message.append(tempUncert);
        message.append(" m\n\n");
        message.append(getApplicationContext().getString(R.string.toshortAddr));
        message.append("\n");
        message.append(adrlines);

        return message.toString();
    }
    // End of getMessage

    // Updates fixtime display every second
    private void registerRelativeFixTime()
    {
        final Handler handler = new Handler(Looper.getMainLooper());
        final String time_header = this.getString(R.string.last_fix_time) + " ";
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                String relative_date = getString(R.string.unknownFix);
                if (fixTime > 0)
                {
                    relative_date = DateUtils.getRelativeTimeSpanString(fixTime,
                        System.currentTimeMillis(), 0, 0).toString();
                }
                String uTime = time_header + relative_date;
                tvUpdatedTime.setText(uTime);
                handler.postDelayed(this, Integer.parseInt(strTime) * 1000L);
            }
        }, Integer.parseInt(strTime) * 1000L);
    }

    private void showSnackbarBlue(String str) // bold cyan text
    {
        baseLayout = findViewById(R.id.baseLayout);
        Snackbar sB = Snackbar.make(baseLayout, str, Snackbar.LENGTH_LONG);
        TextView tv = sB.getView().findViewById(R.id.snackbar_text);
        tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setTextColor(Color.CYAN);
        sB.show();
    }

}
