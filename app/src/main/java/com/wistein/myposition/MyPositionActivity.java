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
 * MyPositionActivity.java
 * Main Activity Class for My Position
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
 * Adopted by wistein for 
 * My Position Ver. 3
 * Copyright 2017, Wilhelm Stein, Germany
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

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
import java.util.TimeZone;

import static android.location.LocationManager.GPS_PROVIDER;

public class MyPositionActivity extends AppCompatActivity implements OnClickListener
{
    private final static String LOG_TAG = "MyPositionActivity";
    private TextView tvDecimalCoord;
    private TextView tvDegreeCoord;
    private TextView tvLocation;
    private TextView tvMessage;
    private TextView tvUpdatedTime;
    private double lat;
    private double lon;
    private double height;
    private double uncertainty;
    private long fixTime; // GPS fix time
    private boolean nonEmpty = false;
    private StringBuffer sb;
    private String addresslines; // formatted string for Address field
    private String addresslines1; // formatted string for message
    private String messageHeader = "";
    private String strTime = "10"; // default update period 10 sec.
    private String emailString = ""; // mail address for OSM query
    
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String provider = GPS_PROVIDER;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_my_location);
        ScrollView baseLayout = (ScrollView) findViewById(R.id.baseLayout);
        assert baseLayout != null;
        getSupportActionBar().setTitle(R.string.app_name);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        df.setTimeZone(TimeZone.getDefault());
        tvDecimalCoord = (TextView) findViewById(R.id.tvDecimalCoord);
        tvDegreeCoord = (TextView) findViewById(R.id.tvDegreeCoord);
        tvUpdatedTime = (TextView) findViewById(R.id.tvUpdatedTime);
        tvLocation = (TextView) findViewById(R.id.tvLocation);
        tvMessage = (TextView) findViewById(R.id.tvMessage);

        ImageView shareLocation = (ImageView) findViewById(R.id.shareLocation);
        ImageView shareDecimal = (ImageView) findViewById(R.id.shareDecimal);
        ImageView shareDegree = (ImageView) findViewById(R.id.shareDegree);
        ImageView shareMessage = (ImageView) findViewById(R.id.shareMessage);

        shareLocation.setClickable(true);
        shareDecimal.setClickable(true);
        shareDegree.setClickable(true);
        shareMessage.setClickable(true);

        shareLocation.setOnClickListener(this);
        shareDecimal.setOnClickListener(this);
        shareDegree.setOnClickListener(this);
        shareMessage.setOnClickListener(this);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        messageHeader = pref.getString("messageHeader", getString(R.string.pref_text));
        strTime = pref.getString("updateFreq", "10");
        emailString = pref.getString("emailString", "");
        
        // Get location service
        int REQUEST_CODE_GPS = 124;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasAccessFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasAccessFineLocationPermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_GPS);
            }
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Create LocationListener object
        locationListener = new LocationListener()
        {
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras)
            {
                // Log.d(LOG_TAG, "onStatusChanged()");
            }

            @Override
            public void onProviderEnabled(String provider)
            {
                // Log.d(LOG_TAG, "onProviderEnabled()");
            }

            @Override
            public void onProviderDisabled(String provider)
            {
                // Log.d(LOG_TAG, "onProviderDisabled()");
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onLocationChanged(Location location)
            {
                lat = location.getLatitude();
                lon = location.getLongitude();
                height = location.getAltitude();
                uncertainty = location.getAccuracy();
                fixTime = location.getTime();

                String nord = getString(R.string.nord);
                String east = getString(R.string.east);
                String west = getString(R.string.west);
                String south = getString(R.string.south);
                String directionNS, directionEW;
                nonEmpty = true;

                if (lat >= 0)
                    directionNS = nord;
                else
                    directionNS = south;

                if (lon >= 0)
                    directionEW = east;
                else
                    directionEW = west;

                sb = new StringBuffer();
                final String time_header = getString(R.string.last_fix_time) + " ";

                String language = Locale.getDefault().toString().substring(0, 2);
                if (language.equals("de"))
                {
                    String lattemp = Float.toString((float) lat);
                    lattemp = lattemp.replace('.', ',');
                    sb.append(lattemp).append(" ").append(directionNS).append(",  ");
                    String lontemp = Float.toString((float) lon);
                    lontemp = lontemp.replace('.', ',');
                    sb.append(lontemp).append(" ").append(directionEW);
                }
                else
                {
                    sb.append(directionNS).append(" ")
                        .append(Float.toString((float) lat)).append(",  ")
                        .append(directionEW).append(" ")
                        .append(Float.toString((float) lon));
                }

                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        final String relative_date = DateUtils.getRelativeTimeSpanString(fixTime, System.currentTimeMillis(), 0).toString();
                        tvDecimalCoord.setText(sb.toString());
                        tvDegreeCoord.setText(toDegree(lat, lon));
                        String uTime = time_header + relative_date;
                        tvUpdatedTime.setText(uTime);

                        RetrieveAddr getXML = new RetrieveAddr();
                        getXML.execute(new LatLong(lat, lon));

                        addresslines = getXML.addresses();
                    }
                });
            } // End of onLocationChanged
        };    // End of locationListener
    }         // End of onCreate

    @Override
    public void onResume()
    {
        super.onResume();

        // Get location service
        try
        {
            locationManager.requestLocationUpdates(GPS_PROVIDER, Integer.parseInt(strTime), 50, locationListener);
        } catch (Exception e)
        {
            //Toast.makeText(this, getString(R.string.no_GPS), Toast.LENGTH_LONG).show();
        }
        boolean defaultTime = true;
        this.registerLocationListener(defaultTime);
        this.registerRelativeFixTime();
    }

    public void onPause()
    {
        super.onPause();

        try
        {
            if (locationManager != null)
            {
                locationManager.removeUpdates(locationListener);
                locationManager = null;
            }
        } catch (Exception e)
        {
            // do nothing
        }
    }

    public void onStop()
    {
        super.onStop();

        try
        {
            if (locationManager != null)
            {
                locationManager.removeUpdates(locationListener);
                locationManager = null;
            }
        } catch (Exception e)
        {
            // do nothing
        }
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
            // Get LocationManager instance
            boolean defaultTime = false;
            this.registerLocationListener(defaultTime);
            this.registerRelativeFixTime();
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
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + lat + "," + lon + "?z=15"));
            startActivity(intent);
            return true;

        case R.id.menu_converter:
            intent = new Intent();
            intent.setClass(MyPositionActivity.this, ConverterActivity.class);
            intent.putExtra("Coordinate", lat + "," + lon);
            startActivityForResult(intent, -1);
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        if (language.equals("de"))
        {
            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");

            String sectemp = new DecimalFormat("#.###").format(convert.getSecond());
            sectemp = sectemp.replace('.', ',');
            stringb.append(sectemp).append("\" ").append(directionNS).append(",  ");

            convert = new LatLonConvert(lon);

            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");

            sectemp = new DecimalFormat("#.###").format(convert.getSecond());
            sectemp = sectemp.replace('.', ',');
            stringb.append(sectemp).append("\" ").append(directionEW);
        }
        else
        {
            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");
            stringb.append(new DecimalFormat("#.###").format(convert.getSecond())).append("\" ").append(directionNS).append(",  ");

            convert = new LatLonConvert(lon);

            stringb.append(new DecimalFormat("#").format(convert.getDegree())).append("\u00b0 ");
            stringb.append(new DecimalFormat("#").format(convert.getMinute())).append("\' ");
            stringb.append(new DecimalFormat("#.###").format(convert.getSecond())).append("\" ").append(directionEW);
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

    // Show message to share
    private static String getMessage(double lat, double lon, double height, double uncertainty, String messageHeader, boolean nonEmpty, String adrlines)
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

        String tempLat = Float.toString((float) lat);
        String tempLon = Float.toString((float) lon);
        String tempHigh = Float.toString((float) height);
        String tempUncert = Float.toString((float) uncertainty);

        String language = Locale.getDefault().toString().substring(0, 2);
        // for "de" replace '.' with ',' in mumbers
        if (language.equals("de"))
        {
            tempLat = tempLat.replace('.', ',');
            tempLon = tempLon.replace('.', ',');
            tempHigh = tempHigh.replace('.', ',');
            tempUncert = tempUncert.replace('.', ',');
        }

        if (!nonEmpty && lat == 0.0 && lon == 0.0)
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
        message.append(": ");
        message.append(tempUncert);
        message.append(" m\n\n");
        message.append(myPosition.getAppContext().getString(R.string.toshortAddr));
        message.append("\n");
        message.append(adrlines);
        
        return message.toString();
    }

    private void registerRelativeFixTime()
    {
        final Handler handler = new Handler();
        final String time_header = this.getString(R.string.last_fix_time) + " ";
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                final String relative_date = DateUtils.getRelativeTimeSpanString(fixTime, System.currentTimeMillis(), 0, 0).toString();
                String uTime = time_header + relative_date;
                tvUpdatedTime.setText(uTime);
                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }

    // Gets last known location
    @SuppressLint("LongLogTag")
    private void registerLocationListener(boolean defaultTime)
    {
        int time;
        if (defaultTime)
        {
            time = Integer.parseInt(strTime) * 1000; // default: 10 sec
        }
        else
        {
            time = 0;
        }
        int distance = 50;
        String nord = getString(R.string.nord);
        String east = getString(R.string.east);
        String west = getString(R.string.west);
        String south = getString(R.string.south);
        String directionNS, directionEW;

        int REQUEST_CODE_GPS = 124;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasAccessFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasAccessFineLocationPermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_GPS);
            }
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(GPS_PROVIDER);

        lat = 0.0;
        lon = 0.0;
        nonEmpty = false;

        if (location != null)
        {
            sb = new StringBuffer("");
            lat = location.getLatitude();
            lon = location.getLongitude();
            height = location.getAltitude();
            fixTime = location.getTime();
            uncertainty = location.getAccuracy();

            if (lat >= 0)
                directionNS = nord;
            else
                directionNS = south;

            if (lon >= 0)
                directionEW = east;
            else
                directionEW = west;

            String lattemp = Float.toString((float) lat);
            lattemp = lattemp.replace('.', ',');
            String lontemp = Float.toString((float) lon);
            lontemp = lontemp.replace('.', ',');
            sb.append(lattemp).append(" ").append(directionNS).append(",  ")
                .append(lontemp).append(" ").append(directionEW);
        }
        else
        {
            sb = new StringBuffer(getString(R.string.unknownAddr));
        }

        final String time_header = this.getString(R.string.last_fix_time) + " ";

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                final String relative_date = DateUtils.getRelativeTimeSpanString(fixTime, System.currentTimeMillis(), 0, 0).toString();
                tvDecimalCoord.setText(sb.toString());
                tvDegreeCoord.setText(toDegree(lat, lon));
                String uTime = time_header + relative_date;
                tvUpdatedTime.setText(uTime);

                RetrieveAddr getXML = new RetrieveAddr();
                getXML.execute(new LatLong(lat, lon));

                addresslines = getXML.addresses();
            }
        });

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance, locationListener);

    } // End of registerLocationListener

    /*********************************************************
     * Get address info from Reverse Geocoder of OpenStreetMap
     */
    private class RetrieveAddr extends AsyncTask<LatLong, Void, String>
    {
        URL url;

        @Override
        protected String doInBackground(LatLong... params)
        {
            if (lat != 0 || lon != 0)
            {
                try
                {
/*                  //prepare for URL shortener
                    StringBuilder urlSB = new StringBuilder();
                    urlSB.append("https://nominatim.openstreetmap.org/reverse?email=");
                    urlSB.append(emailString);
                    urlSB.append("&format=xml&lat=");
                    urlSB.append(Double.toString(lat));
                    urlSB.append("&lon=");
                    urlSB.append(Double.toString(lon));
                    urlSB.append("&zoom=18");
                    urlSB.append("&addressdetails=1");
                    String urlString = urlSB.toString();
*/                    
                    String urlString = "https://nominatim.openstreetmap.org/reverse?email=" + emailString + "&format=xml&lat="
                        + Double.toString(lat) + "&lon=" + Double.toString(lon) + "&zoom=18&addressdetails=1";
                    url = new URL(urlString);
                    //Log.d(LOG_TAG, "urlString: " + urlString); // Log url

                    //HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection(); // https-version?
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

                    String xmlString = convertStreamToString(iStream);
                    Log.d(LOG_TAG, "xmlString: " + xmlString); // Log content of url

                    // parse the XML content 
                    addresslines = "";

                    if (xmlString.contains("<addressparts>"))
                    {
                        int sstart = xmlString.indexOf("<addressparts>") + 14;
                        int send = xmlString.indexOf("</addressparts>");
                        xmlString = xmlString.substring(sstart, send);
                        //Log.d(LOG_TAG, "<addressparts>: " + xmlString);
                        StringBuilder msg = new StringBuilder();

                        // 1. line: building, hotel
                        if (xmlString.contains("<building>"))
                        {
                            sstart = xmlString.indexOf("<building>") + 10;
                            send = xmlString.indexOf("</building>");
                            String building = xmlString.substring(sstart, send);
                            msg.append(building);
                            msg.append("\n ");
                        }
                        if (xmlString.contains("<hotel>"))
                        {
                            sstart = xmlString.indexOf("<hotel>") + 7;
                            send = xmlString.indexOf("</hotel>");
                            String hotel = xmlString.substring(sstart, send);
                            msg.append(hotel);
                            msg.append("\n ");
                        }

                        if (xmlString.contains(">de<") || xmlString.contains(">fr<") || xmlString.contains(">ch<") || xmlString.contains(">at<") || xmlString.contains(">it<"))
                        {
                            // 2. line: road, house-No.
                            if (xmlString.contains("<road>"))
                            {
                                sstart = xmlString.indexOf("<road>") + 6;
                                send = xmlString.indexOf("</road>");
                                String road = xmlString.substring(sstart, send);
                                msg.append(road);
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
                            else
                            {
                                if (xmlString.contains("<road>"))
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
                        else // not de, fr
                        {
                            // 2. line: house, road, house-No.
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

                            //6. line: county
                            if (xmlString.contains("<county>"))
                            {
                                sstart = xmlString.indexOf("<county>") + 8;
                                send = xmlString.indexOf("</county>");
                                String county = xmlString.substring(sstart, send);
                                msg.append(county);
                                msg.append("\n");
                            }

                            // 7. line: state, country 
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
                        addresslines1 = "   " + addresslines;
                        addresslines1 = addresslines1.replace("\n", "\n   ");
                    }

                } catch (IOException e)
                {
                    e.printStackTrace();
                    addresslines = getString(R.string.unknownAddr);
                    return addresslines;
                }
            }
            else
            {
                addresslines = getString(R.string.noAddr);
            }
            return addresslines;
        }

        protected void onPostExecute(String adrlines)
        {
            super.onPostExecute(adrlines);

            try
            {
                tvLocation.setText(adrlines);
                tvMessage.setText(MyPositionActivity.getMessage(lat, lon, height, uncertainty, messageHeader, true, addresslines1));
            } catch (Exception e)
            {
                tvLocation.setText(getString(R.string.noAddr));
                tvMessage.setText(getString(R.string.noAddr));
            }
        }

        public String addresses()
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
                e.printStackTrace();
            } finally
            {
                try
                {
                    is.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    } // end of class RetrieveAddr

    //
    class LatLong
    {
        double lat, lon;

        public LatLong(double lat, double lon)
        {
            this.lat = lat;
            this.lon = lon;
        }
    }

}
