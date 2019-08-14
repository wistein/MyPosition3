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
 ******************
 * HelpDialog.java
 * Custom class for displaying the Help Dialog
 *
 * Copyright 2019, Wilhelm Stein, Germany
 * last edited on 2019-08-12
 */

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class HelpDialog extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener
{
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

        String language = Locale.getDefault().toString().substring(0, 2);
        setContentView(R.layout.activity_help_dialog);

        TextView tv = findViewById(R.id.help_head);
        if (language.equals("de"))
        {
            tv.setText(Utils.fromHtml(readRawTextFile(R.raw.help_head_de)));
        }
        else
        {
            tv.setText(Utils.fromHtml(readRawTextFile(R.raw.help_head)));
        }
        tv.setLinkTextColor(Color.BLUE);
        Linkify.addLinks(tv, Linkify.WEB_URLS);

        tv = findViewById(R.id.help_text);
        if (language.equals("de"))
        {
            tv.setText(Utils.fromHtml(readRawTextFile(R.raw.help_de)));
        }
        else
        {
            tv.setText(Utils.fromHtml(readRawTextFile(R.raw.help)));
        }
        tv.setLinkTextColor(Color.BLUE);
        Linkify.addLinks(tv, Linkify.WEB_URLS);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    private static String readRawTextFile(int id)
    {
        InputStream inputStream = myPosition.getAppContext().getResources().openRawResource(id);
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader buf = new BufferedReader(in);
        String line;
        StringBuilder text = new StringBuilder();
        try
        {
            while ((line = buf.readLine()) != null) text.append(line);
        } catch (IOException e)
        {
            return null;
        }
        return text.toString();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key)
    {
        screenOrientL = pref.getBoolean("screen_Orientation", false);
    }

}
