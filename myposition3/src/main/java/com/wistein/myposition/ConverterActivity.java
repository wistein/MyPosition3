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
 * ConverterActivity.java
 * Converts between Decimal GPS coordinate and DD MM SS coordinate with appropriate
 * language-specific formatting
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
 * Adopted by wistein for MyPosition3
 * Copyright 2019, Wilhelm Stein, Germany
 * last edited on 2019-01-03
 */

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.StringTokenizer;

public class ConverterActivity extends AppCompatActivity implements 
    SharedPreferences.OnSharedPreferenceChangeListener
{
    private EditText tvDecimalLat;
    private EditText tvDecimalLon;
    private EditText tvDegreeLat;
    private EditText tvMinuteLat;
    private EditText tvSecondLat;
    private EditText tvDegreeLon;
    private EditText tvMinuteLon;
    private EditText tvSecondLon;
    private boolean screenOrientL; // option for screen orientation

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        pref.registerOnSharedPreferenceChangeListener(this);
        screenOrientL = pref.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }


        setContentView(R.layout.activity_converter);

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(R.string.title_activity_converter);

        tvDecimalLat = findViewById(R.id.latDecimal);
        tvDecimalLon = findViewById(R.id.lonDecimal);

        tvDegreeLat = findViewById(R.id.degreelat);
        tvMinuteLat = findViewById(R.id.minutelat);
        tvSecondLat = findViewById(R.id.secondlat);

        tvDegreeLon = findViewById(R.id.degreelon);
        tvMinuteLon = findViewById(R.id.minutelon);
        tvSecondLon = findViewById(R.id.secondlon);
        String coordinates = (String) getIntent().getSerializableExtra("Coordinate");
        StringTokenizer token = new StringTokenizer(coordinates, ", ");

        String tlat, tlon;
        tlat = token.nextToken();
        tlon = token.nextToken();

        new DecimalFormat("#.#####").format(Double.parseDouble(tlon));
        tvDecimalLat.setText(new DecimalFormat("#.#####").format(Double.parseDouble(tlat)));
        tvDecimalLon.setText(new DecimalFormat("#.#####").format(Double.parseDouble(tlon)));

        this.toDegree();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_converter, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.menu_toDecimal:
            this.toDecimal();
            break;

        case R.id.menu_toDegree:
            this.toDegree();
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toDegree()
    {
        try
        {
            String lattemp = (tvDecimalLat.getText().toString());
            String lontemp = (tvDecimalLon.getText().toString());
            lattemp = lattemp.replace(',', '.');
            lontemp = lontemp.replace(',', '.');
            double lat = Double.parseDouble(lattemp);
            double lon = Double.parseDouble(lontemp);

            LatLonConvert convert = new LatLonConvert(lat);

            String tvtemp;
            tvtemp = "" + new DecimalFormat("#").format(convert.getDegree());
            tvDegreeLat.setText(tvtemp);
            tvtemp = "" + new DecimalFormat("#").format(convert.getMinute());
            tvMinuteLat.setText(tvtemp);
            tvtemp = "" + new DecimalFormat("#.##").format(convert.getSecond());
            tvSecondLat.setText(tvtemp);

            convert = new LatLonConvert(lon);
            tvtemp = "" + new DecimalFormat("#").format(convert.getDegree());
            tvDegreeLon.setText(tvtemp);
            tvtemp = "" + new DecimalFormat("#").format(convert.getMinute());
            tvMinuteLon.setText(tvtemp);
            tvtemp = "" + new DecimalFormat("#.##").format(convert.getSecond());
            tvSecondLon.setText(tvtemp);
        } catch (NumberFormatException nfe)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.degree)
                + " " + getString(R.string.invValue), Toast.LENGTH_SHORT).show();
        }
    }

    private void toDecimal()
    {
        try
        {
            String deglattemp = (tvDegreeLat.getText().toString());
            String minlattemp = (tvMinuteLat.getText().toString());
            String seclattemp = (tvSecondLat.getText().toString());
            
            // for correct calculation replace ',' with '.' for German locale
            deglattemp = deglattemp.replace(',', '.');
            minlattemp = minlattemp.replace(',', '.');
            seclattemp = seclattemp.replace(',', '.');

            LatLonConvert convert = new LatLonConvert(
                Double.parseDouble(deglattemp),
                Double.parseDouble(minlattemp),
                Double.parseDouble(seclattemp)
            );

            String tvtemp;
            tvtemp = "" + new DecimalFormat("#.#####").format(convert.getDecimal());
            tvDecimalLat.setText(tvtemp);

            String deglontemp = (tvDegreeLon.getText().toString());
            String minlontemp = (tvMinuteLon.getText().toString());
            String seclontemp = (tvSecondLon.getText().toString());

            // for correct calculation replace ',' with '.' for German locale
            deglontemp = deglontemp.replace(',', '.');
            minlontemp = minlontemp.replace(',', '.');
            seclontemp = seclontemp.replace(',', '.');

            convert = new LatLonConvert(
                Double.parseDouble(deglontemp),
                Double.parseDouble(minlontemp),
                Double.parseDouble(seclontemp)
            );

            tvtemp = "" + new DecimalFormat("#.#####").format(convert.getDecimal());
            tvDecimalLon.setText(tvtemp);
        } catch (NumberFormatException nfe)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.decimal)
                + " " + getString(R.string.invValue), Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key)
    {
        screenOrientL = pref.getBoolean("screen_Orientation", false);
    }

}
