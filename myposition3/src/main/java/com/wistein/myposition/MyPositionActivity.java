package com.wistein.myposition;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

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
 * Adopted by wistein for MyPosition3
 * Copyright 2019-2024, Wilhelm Stein, Bonn, Germany
 * last edited on 2024-12-22
 */
public class MyPositionActivity
    extends AppCompatActivity
    implements OnClickListener,
               PermissionsDialogFragment.PermissionsGrantedCallback
{
    private static final String TAG = "MyPositionAct";
    private TextView tvDecimalCoord;
    private TextView tvDegreeCoord;
    public TextView tvLocation;
    public TextView tvMessage;
    private TextView tvUpdatedTime;
    public static String addresslines; // formatted string for Address field
    private String messageHeader = ""; // 1st line in mail message
    private String emailString = "";   // mail address for OSM query
    private boolean screenOrientL;     // option for screen orientation
    private boolean darkScreen;        // Option for dark screen background
    private boolean showToast;         // option to show toast with height info

    // mapLocal is option to select local app (true) or online map (false).
    // When GAPPS are present MyPosition3 shows location always even online on Google Maps,
    //   without GAPPS MyPosition3 uses a local mapping app to show the location
    private boolean mapLocal;

    private SharedPreferences prefs;

    /* Permission Handling
     * variable 'modePerm' controls Permission dispatcher mode:
     * 1 = use location service
     * 2 = end location service
     */
    private int modePerm;

    /* Permission Handling
     * variable 'permLocGiven' contains the initial location permission state that
     * controls stopping the location listener after permission has been changed:
     * - Stop listener if permission was denied after listener start
     * - don't stop listener if permission was allowed later and listener has not been started
     */
    private boolean permLocGiven;

    // Location info handling
    private double lat, lon, uncertainty;
    private double height = 0;
    private long fixTime = 0; // GPS fix time
    private final String strTime = "10"; // option to set GPS polling time, default 10 sec
    LocationService locationService;

    public boolean doubleBackToExitPressedTwice = false;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        if (MyDebug.LOG) Log.i(TAG, "122, onCreate");

        prefs = myPosition.getPrefs();

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
        ScrollView baseLayout = findViewById(R.id.baseLayout);
        assert baseLayout != null;
        try
        {
            Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);
        } catch (NullPointerException e)
        {
            // do nothing
        }

        // new onBackPressed logic
        final Handler m1Handler = new Handler();
        final Runnable r1 = () -> doubleBackToExitPressedTwice = false;

        OnBackPressedCallback callback = new OnBackPressedCallback(true)
        {
            @Override
            public void handleOnBackPressed()
            {
                if (doubleBackToExitPressedTwice)
                {
                    finishAndRemoveTask();
                }
                else
                {
                    doubleBackToExitPressedTwice = true;

                    Toast t = new Toast(getApplicationContext());
                    LayoutInflater inflater = getLayoutInflater();

                    @SuppressLint("InflateParams")
                    View toastView = inflater.inflate(R.layout.toast_view, null);
                    TextView textView = toastView.findViewById(R.id.toast);
                    textView.setText(R.string.back_twice);

                    t.setView(toastView);
                    t.setDuration(Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                    t.show();

                    m1Handler.postDelayed(r1, 1500);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    // End of onCreate

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume()
    {
        super.onResume();

        if (MyDebug.LOG) Log.i(TAG, "204, onResume");

        messageHeader = getString(R.string.msg_text);
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
        darkScreen = prefs.getBoolean("dark_Screen", false);
        mapLocal = prefs.getBoolean("map_Local", false);
        showToast = prefs.getBoolean("show_Toast", false);
        permLocGiven = prefs.getBoolean("permLoc_Given", false);
        emailString = prefs.getString("email_String", "");

        if (MyDebug.LOG) Log.i(TAG, "214, onResume, darkScreen: " + darkScreen);
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

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        df.setTimeZone(TimeZone.getDefault());
        tvDecimalCoord = findViewById(R.id.tvDecimalCoord);
        tvDegreeCoord = findViewById(R.id.tvDegreeCoord);
        tvUpdatedTime = findViewById(R.id.tvUpdatedTime);
        tvLocation = findViewById(R.id.tvLocation);
        tvMessage = findViewById(R.id.tvMessage);

        ImageView shareLocation = findViewById(R.id.shareLocation);
        ImageView shareDecimal = findViewById(R.id.shareDecimal);
        ImageView shareDegree = findViewById(R.id.shareDegree);
        ImageView shareMessage = findViewById(R.id.shareMessage);

        shareLocation.setClickable(true);
        shareDecimal.setClickable(true);
        shareDegree.setClickable(true);
        shareMessage.setClickable(true);

        shareLocation.setOnClickListener(this);
        shareDecimal.setOnClickListener(this);
        shareDegree.setOnClickListener(this);
        shareMessage.setOnClickListener(this);

        // check initial location permission state
        permLocGiven = isPermissionGranted();

        // store location permission state
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();

        // Get location with permissions check
        modePerm = 1; // get location
        permissionCaptureFragment();
        registerRelativeFixTime();
    }
    // End of onResume()

    @Override
    public void onPause()
    {
        super.onPause();

        if (MyDebug.LOG) Log.i(TAG, "276, onPause");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();
    }

    public void onStop()
    {
        super.onStop();

        if (MyDebug.LOG) Log.i(TAG, "286, onStop");

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();

        // Stop RetrieveAddrRunner
        WorkManager.getInstance(this).cancelAllWork();
    }

    public void onDestroy()
    {
        super.onDestroy();

        if (MyDebug.LOG) Log.i(TAG, "300, onDestroy");
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
            modePerm = 1; // get location

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
            intent = new Intent();
            intent.setClass(MyPositionActivity.this, ConverterActivity.class);
            intent.putExtra("Coordinate", lat + "," + lon);
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }

        return super.onOptionsItemSelected(item);
    }
    // end of onOptionsItemSelected()

    // Part of location permission handling
    @Override
    public void permissionCaptureFragment()
    {
        if (isPermissionGranted())
        {
            switch (modePerm)
            {
                case 1 ->
                { // get location
                    if (permLocGiven) // location permission state after start
                        getLoc();
                }
                case 2 ->
                { // stop location service
                    if (permLocGiven)
                    {
                        locationService.stopListener();
                        if (MyDebug.LOG) Log.i(TAG, "393, Stop locationService");
                    }
                }
            }
        }
        else
        {
            if (modePerm == 1)
                PermissionsDialogFragment.newInstance().show(getSupportFragmentManager(), PermissionsDialogFragment.class.getName());
        }
    }

    // if API level > 23 test for permissions granted
    private boolean isPermissionGranted()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // get the location data
    public void getLoc()
    {
        locationService = new LocationService(this);

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
            final Handler m2Handler = new Handler();
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

        if (showToast)
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
        if (language.equals("de") || language.equals("es") || language.equals("fr") || language.equals("it") || language.equals("nl") || language.equals("pt"))
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
        final Handler handler = new Handler();
        final String time_header = this.getString(R.string.last_fix_time) + " ";
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                String relative_date = getString(R.string.unknownFix);
                if (fixTime > 0)
                {
                    relative_date = DateUtils.getRelativeTimeSpanString(fixTime, System.currentTimeMillis(), 0, 0).toString();
                }
                String uTime = time_header + relative_date;
                tvUpdatedTime.setText(uTime);
                handler.postDelayed(this, Integer.parseInt(strTime) * 1000L);
            }
        }, Integer.parseInt(strTime) * 1000L);
    }

}
