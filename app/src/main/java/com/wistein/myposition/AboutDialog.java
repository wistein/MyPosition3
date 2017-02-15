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
 * MyLocation 1.1c for Android <mypapit@gmail.com> (9w2wtf)
 * Copyright 2012 Mohammad Hafiz bin Ismail. All rights reserved.
 *
 * Info url :
 * http://code.google.com/p/mylocation/
 * http://kirostudio.com
 * http://blog.mypapit.net/
 * 
 * 
 * AboutDialog.java
 * Custom class for displaying About Dialog
 * My GPS Location Tool
 */

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class AboutDialog extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String language = Locale.getDefault().toString().substring(0, 2);
        setContentView(R.layout.activity_about_dialog);

        TextView tv = (TextView) findViewById(R.id.legal_text);
        if (language.equals("de"))
        {
            tv.setText(Html.fromHtml(readRawTextFile(R.raw.legal_de)));
        }
        else
        {
            tv.setText(Html.fromHtml(readRawTextFile(R.raw.legal)));
        }
        tv.setLinkTextColor(Color.BLUE);
        Linkify.addLinks(tv, Linkify.WEB_URLS);
        
        tv = (TextView) findViewById(R.id.info_text);
        if (language.equals("de"))
        {
            tv.setText(Html.fromHtml(readRawTextFile(R.raw.info_de)));
        }
        else
        {
            tv.setText(Html.fromHtml(readRawTextFile(R.raw.info)));
        }
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
}