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
 ************************* 
 * MyPositionActivity.java
 * Main Activity Class for MyPosition3
 *
 * Based on
 * MyLocation 1.1c for Android <mypapit@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 *
 * Info url:
 * http://code.google.com/p/mylocation/
 * http://kirostudio.com
 * http://blog.mypapit.net/
 *
 * Adopted by wistein for MyPosition3
 * Copyright 2019, Wilhelm Stein, Germany
 * last edited on 2019-08-14
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.wistein.egm.EarthGravitationalModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MyPositionActivity extends AppCompatActivity implements OnClickListener, PermissionsDialogFragment.PermissionsGrantedCallback
{
    private final static String LOG_TAG = "MyPositionActivity";
    private TextView tvDecimalCoord;
    private TextView tvDegreeCoord;
    public TextView tvLocation;
    public TextView tvMessage;
    private TextView tvUpdatedTime;
    private StringBuffer sb;
    private String addresslines; // formatted string for Address field
    private String addresslines1; // formatted string for message
    private String messageHeader = ""; // 1st line in mail message
    private String emailString = ""; // mail address for OSM query
    private boolean screenOrientL; // option for screen orientation
    private boolean showToast; // option to show toast with height info
    private boolean mapLocal; // option to select local or online map
    public boolean doubleBackToExitPressedOnce;
    private SharedPreferences pref;
    
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
    private String strTime = "10"; // option to set GPS polling time, default 10 sec
    LocationService locationService;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        emailString = pref.getString("email_String", "");

        screenOrientL = pref.getBoolean("screen_Orientation", false);
        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

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

    } // End of onCreate

    @Override
    public void onResume()
    {
        super.onResume();

        messageHeader = getString(R.string.msg_text);
        screenOrientL = pref.getBoolean("screen_Orientation", false);
        mapLocal = pref.getBoolean("map_Local", false);
        showToast = pref.getBoolean("show_Toast", false);
        permLocGiven = pref.getBoolean("permLoc_Given", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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

        // Get location with permissions check
        locationService = new LocationService(this);

        // check initial location permission state
        permLocGiven = isPermissionGranted();

        // store location permission state
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();

        // Get location with permissions check
        modePerm = 1; // get location
        permissionCaptureFragment();
        registerRelativeFixTime();

    }

    @Override
    public void onPause()
    {
        super.onPause();

        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("permLoc_Given", permLocGiven);
        editor.apply();

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();
    }

    @Override
    public void onStop()
    {
        super.onStop();

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();
    }


    public void onDestroy()
    {
        super.onDestroy();

        // Stop location service with permissions check
        modePerm = 2;
        permissionCaptureFragment();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_my_location, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;
        switch (item.getItemId())
        {
        case R.id.menu_getpos:
            // Get location with permissions check
            modePerm = 1; // get location
            // call DummyActivity to reenter MyPositionActivity to get the new position
            intent = new Intent(MyPositionActivity.this, DummyActivity.class);
            startActivity(intent);
            return true;

        case R.id.menu_help:
            intent = new Intent(MyPositionActivity.this, HelpDialog.class);
            startActivity(intent);
            return true;

        case R.id.menu_about:
            intent = new Intent(MyPositionActivity.this, AboutDialog.class);
            startActivity(intent);
            return true;

        case R.id.menu_settings:
            intent = new Intent(MyPositionActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;

        case R.id.menu_viewmap:
            if (mapLocal)
            {
                // when GAPPS are present MyPosition3 shows location online on Google Maps
                // without GAPPS MyPosition3 uses a local mapping app to show the location
                String geo = "geo:" + lat + "," + lon + "?z=17";
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geo));
            }
            else
            {
                // use browser or other web app to show location in OpenStreetMap
                String urlView = "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=17/" + lat + "/" + lon;
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlView));
            }

            //Make sure there is an app to handle this intent
            if (intent.resolveActivity(getPackageManager()) != null)
            {
                if (MyDebug.LOG)
                {
                    String app;
                    app = intent.resolveActivity(getPackageManager()).toString(); // used only in debug mode
                    Toast.makeText(this, app, Toast.LENGTH_LONG).show();
                    Log.d(LOG_TAG, app);
                }
                startActivity(intent);
            }
            else
                Toast.makeText(this, getString(R.string.t_noapp), Toast.LENGTH_LONG).show();
            return true;

        case R.id.menu_converter:
            intent = new Intent();
            intent.setClass(MyPositionActivity.this, ConverterActivity.class);
            intent.putExtra("Coordinate", lat + "," + lon);
            startActivityForResult(intent, -1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    } // end of onOptionsItemSelected

    // Part of location permission handling
    @Override
    public void permissionCaptureFragment()
    {
        if (isPermissionGranted())
        {
            switch (modePerm)
            {
            case 1: // get location
                if (permLocGiven) // location permission state after start
                    getLoc();
                break;

            case 2: // stop location service
                if (permLocGiven)
                {
                    locationService.stopListener();
                    if (MyDebug.LOG)
                        Toast.makeText(this, "Stop locationService", Toast.LENGTH_SHORT).show();
                }
                break;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            // handle permissions for Build.VERSION_CODES < M here
            return true;
        }
    }

    // get the location data
    public void getLoc()
    {
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

            sb = new StringBuffer();

            String lattemp = String.format("%.5f", lat); // warnings not relevant here
            String lontemp = String.format("%.5f", lon);
            String heighttemp = String.format("%.1f", height);
            String uncerttemp = String.format("%.1f", uncertainty);

            String language = Locale.getDefault().toString().substring(0, 2);

            // for "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in mumbers
            if (language.equals("de") || language.equals("es") || language.equals("fr") || language.equals("it") || language.equals("nl") || language.equals("pt"))
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
            sb = new StringBuffer(getString(R.string.posnotknown));
        }

        // get reverse geocoding
        if (locationService.canGetLocation() && (lat != 0 || lon != 0))
        {
            final String time_header = this.getString(R.string.last_fix_time) + " ";

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String relative_date = getString(R.string.unknownFix);
                    if (fixTime > 0)
                    {
                        relative_date = DateUtils.getRelativeTimeSpanString(fixTime, System.currentTimeMillis(), 0, 0).toString();
                    }
                    tvDecimalCoord.setText(sb.toString());
                    tvDegreeCoord.setText(toDegree(lat, lon));
                    String uTime = time_header + relative_date;
                    tvUpdatedTime.setText(uTime);

                    // call reverse geocoding
                    URL url;
                    String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString + "&format=xml&lat="
                        + lat + "&lon=" + lon + "&zoom=18&addressdetails=1";
                    try
                    {
                        url = new URL(urlString);
                        RetrieveAddr getXML = new RetrieveAddr();
                        getXML.execute(url);
                        addresslines = getXML.addresses();
                    } catch (IOException e)
                    {
                        // do nothing
                    }
                }
            });
        }
        else
            addresslines = getString(R.string.noAddr);

    } // end of getLoc

    private double correctHeight(double lat, double lon, double gpsHeight)
    {
        double corrHeight;
        double nnHeight;

        EarthGravitationalModel gh = new EarthGravitationalModel();
        try
        {
            gh.load(); // load the WGS84 correction coefficient table egm180.txt
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
            String corrtemp = String.format("%.1f", corrHeight); // warnings not relevant here
            String gpstemp = String.format("%.1f", gpsHeight);
            String nntemp = String.format("%.1f", nnHeight);

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

        // for "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in mumbers
        if (language.equals("de") || language.equals("es") || language.equals("fr") || language.equals("it") || language.equals("nl") || language.equals("pt"))
        {
            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");

            String sectemp = new DecimalFormat("#.#").format(convert.getSecond());
            sectemp = sectemp.replace('.', ',');
            stringb.append(sectemp).append("\" ").append(directionNS).append(",  ");

            convert = new LatLonConvert(lon);

            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");

            sectemp = new DecimalFormat("#.#").format(convert.getSecond());
            sectemp = sectemp.replace('.', ',');
            stringb.append(sectemp).append("\" ").append(directionEW);
        }
        else
        {
            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");
            stringb.append(new DecimalFormat("#.#").format(convert.getSecond())).append("\" ").append(directionNS).append(",  ");

            convert = new LatLonConvert(lon);

            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");
            stringb.append(new DecimalFormat("#.#").format(convert.getSecond())).append("\" ").append(directionEW);
        }

        return stringb.toString();
    }

    /*
     * Share button clicked next to one of the text boxes
     */
    @Override
    public void onClick(View view)
    {
        Intent intent;
        intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TITLE, "My Location");
        intent.setType("text/plain");
        switch (view.getId())
        {
        case R.id.shareLocation:
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.myLoc) + "\n  " + tvLocation.getText());
            break;

        case R.id.shareDecimal:
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.myPos) + "\n  " + tvDecimalCoord.getText());
            break;

        case R.id.shareDegree:
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.myPos) + "\n  " + tvDegreeCoord.getText());
            break;

        case R.id.shareMessage:
            intent.putExtra(Intent.EXTRA_TEXT, tvMessage.getText());
            break;
        }

        startActivity(Intent.createChooser(intent, "Share via"));
    }

    @Override
    public void onBackPressed()
    {
        if (doubleBackToExitPressedOnce)
        {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.back_twice, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable()
        {

            @Override
            public void run()
            {
                doubleBackToExitPressedOnce = false;
            }
        }, 1000);
    }

    // Show message to share
    private static String getMessage(double lat, double lon, double height, double uncertainty, String messageHeader, String adrlines)
    {
        StringBuilder message = new StringBuilder();
        String geoLoc = myPosition.getAppContext().getString(R.string.geoloc);
        String uncert = myPosition.getAppContext().getString(R.string.uncert);
        String nord = myPosition.getAppContext().getString(R.string.nord);
        String east = myPosition.getAppContext().getString(R.string.east);
        String west = myPosition.getAppContext().getString(R.string.west);
        String south = myPosition.getAppContext().getString(R.string.south);
        String lati = myPosition.getAppContext().getString(R.string.lati);
        String longi = myPosition.getAppContext().getString(R.string.longi);
        String directionNS, directionEW;
        String high = myPosition.getAppContext().getString(R.string.height);

        String tempLat = String.format("%.5f", lat); // warnings not relevant here
        String tempLon = String.format("%.5f", lon);
        String tempHigh = String.format("%.1f", height);
        String tempUncert = String.format("%.1f", uncertainty);

        String language = Locale.getDefault().toString().substring(0, 2);
        // for "de", "es", "fr", "it", "nl", "pt" replace '.' with ',' in mumbers
        if (language.equals("de") || language.equals("es") || language.equals("fr") || language.equals("it") || language.equals("nl") || language.equals("pt"))
        {
            tempLat = tempLat.replace('.', ',');
            tempLon = tempLon.replace('.', ',');
            tempHigh = tempHigh.replace('.', ',');
            tempUncert = tempUncert.replace('.', ',');
        }

        if (lat == 0.0 && lon == 0.0)
        {
            return myPosition.getAppContext().getString(R.string.posnotknown);
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
        message.append(myPosition.getAppContext().getString(R.string.toshortAddr));
        message.append("\n");
        message.append(adrlines);

        return message.toString();
    } // end of getMessage

    // updates fixtime display every second
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
                handler.postDelayed(this, Integer.parseInt(strTime) * 1000);
            }
        }, Integer.parseInt(strTime)* 1000);
    }
    
    /*********************************************************
     * Get address info from Reverse Geocoder of OpenStreetMap
     */
    @SuppressLint("StaticFieldLeak")
    private class RetrieveAddr extends AsyncTask<URL, Void, String>
    {
        final String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString + "&format=xml&lat="
            + lat + "&lon=" + lon + "&zoom=18&addressdetails=1";

        @Override
        protected String doInBackground(URL... params)
        {
            URL url = params[0];
            String xmlString;
            
            try
            {
                if (MyDebug.LOG)
                    Log.d(LOG_TAG, "urlString: " + urlString); // Log url

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoInput(true);
                urlConnection.connect();

                int status = urlConnection.getResponseCode();
                if (status >= 400) // Error
                {
                    addresslines = (getString(R.string.errAddr));
                    return addresslines;
                }

                // get the XML from input stream
                InputStream iStream = urlConnection.getInputStream();

                xmlString = convertStreamToString(iStream);
                if (MyDebug.LOG)
                    Log.d(LOG_TAG, "xmlString: " + xmlString); // Log content of url

                // parse the XML content 
                addresslines = "";

                if (xmlString.contains("<addressparts>"))
                {
                    int sstart = xmlString.indexOf("<addressparts>") + 14;
                    int send = xmlString.indexOf("</addressparts>");
                    xmlString = xmlString.substring(sstart, send);
                    if (MyDebug.LOG)
                        Log.d(LOG_TAG, "<addressparts>: " + xmlString);
                    StringBuilder msg = new StringBuilder();

                    // 1. line: building, viewpoint, hotel or guesthouse
                    if (xmlString.contains("<building>"))
                    {
                        sstart = xmlString.indexOf("<building>") + 10;
                        send = xmlString.indexOf("</building>");
                        String building = xmlString.substring(sstart, send);
                        msg.append(building);
                        msg.append("\n");
                    }
                    if (xmlString.contains("<viewpoint>"))
                    {
                        sstart = xmlString.indexOf("<viewpoint>") + 11;
                        send = xmlString.indexOf("</viewpoint>");
                        String viewpoint = xmlString.substring(sstart, send);
                        msg.append(viewpoint);
                        msg.append("\n");
                    }
                    if (xmlString.contains("<hotel>"))
                    {
                        sstart = xmlString.indexOf("<hotel>") + 7;
                        send = xmlString.indexOf("</hotel>");
                        String hotel = xmlString.substring(sstart, send);
                        msg.append(hotel);
                        msg.append("\n");
                    }
                    if (xmlString.contains("<guest_house>"))
                    {
                        sstart = xmlString.indexOf("<guest_house>") + 13;
                        send = xmlString.indexOf("</guest_house>");
                        String guest_house = xmlString.substring(sstart, send);
                        msg.append(guest_house);
                        msg.append("\n");
                    }

                    if (xmlString.contains(">de<") || xmlString.contains(">fr<") || xmlString.contains(">ch<") || xmlString.contains(">at<") || xmlString.contains(">it<"))
                    {
                        // 2. line: road or street, house-No.
                        if (xmlString.contains("<road>"))
                        {
                            sstart = xmlString.indexOf("<road>") + 6;
                            send = xmlString.indexOf("</road>");
                            String road = xmlString.substring(sstart, send);
                            msg.append(road);
                            msg.append(" ");
                        }
                        if (xmlString.contains("<street>"))
                        {
                            sstart = xmlString.indexOf("<street>") + 8;
                            send = xmlString.indexOf("</street>");
                            String street = xmlString.substring(sstart, send);
                            msg.append(street);
                            msg.append(" ");
                        }
                        if (xmlString.contains("<house_number>"))
                        {
                            sstart = xmlString.indexOf("<house_number>") + 14;
                            send = xmlString.indexOf("</house_number>");
                            String house_number = xmlString.substring(sstart, send);
                            msg.append(house_number);
                            msg.append("\n");
                        }
                        else // without house-No.
                        {
                            if (xmlString.contains("<road>") || xmlString.contains("<street>"))
                                msg.append("\n");
                        }

                        // 3. line: city_district, suburb
                        if (xmlString.contains("<city_district>"))
                        {
                            sstart = xmlString.indexOf("<city_district>") + 15;
                            send = xmlString.indexOf("</city_district>");
                            String city_district = xmlString.substring(sstart, send);
                            msg.append(city_district);
                            if (xmlString.contains("<suburb>"))
                            {
                                msg.append(" - ");
                            }
                            else
                            {
                                msg.append("\n");
                            }
                        }
                        if (xmlString.contains("<suburb>"))
                        {
                            sstart = xmlString.indexOf("<suburb>") + 8;
                            send = xmlString.indexOf("</suburb>");
                            String suburb = xmlString.substring(sstart, send);
                            msg.append(suburb);
                            msg.append("\n");
                        }

                        // 4. line: postcode village, town, city, county
                        if (xmlString.contains("<postcode>"))
                        {
                            sstart = xmlString.indexOf("<postcode>") + 10;
                            send = xmlString.indexOf("</postcode>");
                            String postcode = xmlString.substring(sstart, send);
                            msg.append(postcode);
                            msg.append(" ");
                        }

                        if (xmlString.contains("<village>"))
                        {
                            sstart = xmlString.indexOf("<village>") + 9;
                            send = xmlString.indexOf("</village>");
                            String village = xmlString.substring(sstart, send);
                            msg.append(village);
                            msg.append("\n");
                        }

                        if (xmlString.contains("<town>"))
                        {
                            sstart = xmlString.indexOf("<town>") + 6;
                            send = xmlString.indexOf("</town>");
                            String town = xmlString.substring(sstart, send);
                            msg.append(town);
                            msg.append("\n");
                        }

                        if (xmlString.contains("<city>"))
                        {
                            sstart = xmlString.indexOf("<city>") + 6;
                            send = xmlString.indexOf("</city>");
                            String city = xmlString.substring(sstart, send);
                            msg.append(city);
                            msg.append("\n");
                        }

                        if (xmlString.contains("<county>"))
                        {
                            sstart = xmlString.indexOf("<county>") + 8;
                            send = xmlString.indexOf("</county>");
                            String county = xmlString.substring(sstart, send);
                            msg.append(county);
                            msg.append("\n");
                        }

                        // 5. line: state, country 
                        if (xmlString.contains("<state>"))
                        {
                            sstart = xmlString.indexOf("<state>") + 7;
                            send = xmlString.indexOf("</state>");
                            String state = xmlString.substring(sstart, send);
                            msg.append(state);
                            msg.append("\n");
                        }
                        if (xmlString.contains("<country>"))
                        {
                            sstart = xmlString.indexOf("<country>") + 9;
                            send = xmlString.indexOf("</country>");
                            String country = xmlString.substring(sstart, send);
                            msg.append(country);
                        }
                    }
                    else // not at, ch, de, fr, it
                    {
                        // 2. line: house, house-No., road or street
                        if (xmlString.contains("<house>"))
                        {
                            sstart = xmlString.indexOf("<house>") + 7;
                            send = xmlString.indexOf("</house>");
                            String house = xmlString.substring(sstart, send);
                            msg.append(house);
                            msg.append(" ");
                        }
                        if (xmlString.contains("<house_number>"))
                        {
                            sstart = xmlString.indexOf("<house_number>") + 14;
                            send = xmlString.indexOf("</house_number>");
                            String house_number = xmlString.substring(sstart, send);
                            msg.append(house_number);
                            msg.append(" ");
                        }
                        if (xmlString.contains("<road>"))
                        {
                            sstart = xmlString.indexOf("<road>") + 6;
                            send = xmlString.indexOf("</road>");
                            String road = xmlString.substring(sstart, send);
                            msg.append(road);
                            msg.append("\n");
                        }
                        if (xmlString.contains("<street>"))
                        {
                            sstart = xmlString.indexOf("<street>") + 8;
                            send = xmlString.indexOf("</street>");
                            String street = xmlString.substring(sstart, send);
                            msg.append(street);
                            msg.append("\n");
                        }

                        // 3. line: suburb
                        if (xmlString.contains("<suburb>"))
                        {
                            sstart = xmlString.indexOf("<suburb>") + 8;
                            send = xmlString.indexOf("</suburb>");
                            String suburb = xmlString.substring(sstart, send);
                            msg.append(suburb);
                            msg.append("\n");
                        }

                        // 4. line: village, town
                        if (xmlString.contains("<village>"))
                        {
                            sstart = xmlString.indexOf("<village>") + 9;
                            send = xmlString.indexOf("</village>");
                            String village = xmlString.substring(sstart, send);
                            msg.append(village);
                            msg.append("\n");
                        }
                        if (xmlString.contains("<town>"))
                        {
                            sstart = xmlString.indexOf("<town>") + 6;
                            send = xmlString.indexOf("</town>");
                            String town = xmlString.substring(sstart, send);
                            msg.append(town);
                            msg.append("\n");
                        }

                        // 5. line: city
                        if (xmlString.contains("<city>"))
                        {
                            sstart = xmlString.indexOf("<city>") + 6;
                            send = xmlString.indexOf("</city>");
                            String city = xmlString.substring(sstart, send);
                            msg.append(city);
                            msg.append("\n");
                        }

                        //6. line: county, state_district
                        if (xmlString.contains("<county>"))
                        {
                            sstart = xmlString.indexOf("<county>") + 8;
                            send = xmlString.indexOf("</county>");
                            String county = xmlString.substring(sstart, send);
                            msg.append(county);
                            msg.append("\n");
                        }

                        if (xmlString.contains("<state_district>"))
                        {
                            sstart = xmlString.indexOf("<state_district>") + 16;
                            send = xmlString.indexOf("</state_district>");
                            String state_district = xmlString.substring(sstart, send);
                            msg.append(state_district);
                            msg.append("\n");
                        }

                        // 7. line: state or country, postcode 
                        if (xmlString.contains("<state>"))
                        {
                            sstart = xmlString.indexOf("<state>") + 7;
                            send = xmlString.indexOf("</state>");
                            String state = xmlString.substring(sstart, send);
                            msg.append(state);
                            msg.append("\n");
                        }
                        if (xmlString.contains("<country>"))
                        {
                            sstart = xmlString.indexOf("<country>") + 9;
                            send = xmlString.indexOf("</country>");
                            String country = xmlString.substring(sstart, send);
                            msg.append(country);
                            msg.append(", ");
                        }

                        if (xmlString.contains("<postcode>"))
                        {
                            sstart = xmlString.indexOf("<postcode>") + 10;
                            send = xmlString.indexOf("</postcode>");
                            String postcode = xmlString.substring(sstart, send);
                            msg.append(postcode);
                        }
                    }

                    addresslines = msg.toString();
                    // format for TextView tvMessage
                    addresslines1 = "   " + addresslines;
                    addresslines1 = addresslines1.replace("\n", "\n   ");
                }

            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(LOG_TAG, "Problem with address handling: " + e.toString());
                addresslines = getString(R.string.unknownAddr);
                return addresslines;
            }

            return addresslines;
        }

        protected void onPostExecute(String adrlines)
        {
            super.onPostExecute(adrlines);

            try
            {
                tvLocation.setText(adrlines);
                tvMessage.setText(MyPositionActivity.getMessage(lat, lon, height, uncertainty, messageHeader, addresslines1));
            } catch (Exception e)
            {
                tvLocation.setText(getString(R.string.noAddr));
                tvMessage.setText(getString(R.string.noAddr));
            }
        }

        String addresses()
        {
            return addresslines;
        }

        private String convertStreamToString(InputStream is)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line;
            try
            {
                while ((line = reader.readLine()) != null)
                {
                    sb.append(line).append('\n');
                }
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(LOG_TAG, "Problem converting Stream to String: " + e.toString());
            } finally
            {
                try
                {
                    is.close();
                } catch (IOException e)
                {
                    if (MyDebug.LOG)
                        Log.e(LOG_TAG, "Problem closing InputStream: " + e.toString());
                }
            }
            return sb.toString();
        }
    } // end of class RetrieveAddr

}
