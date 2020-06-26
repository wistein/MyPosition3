/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package com.wistein.myposition;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**************************************************************************************
 * Dummy to reenter MyPositionActivity for location request
 * Created by wmstein on 2019-08-12
 */
public class DummyActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        exit();
    }

    public void exit()
    {
        super.finish();
    }
    
}
