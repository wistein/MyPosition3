/*
 * Copyright (c) 2017. Wilhelm Stein, Bonn, Germany.
 */

package com.wistein.myposition;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;

/**
 * Class Utils cares for Android versions compatibility
 *  - in AboutDialog fromHtml
 *  
 * Created by wistein on 25.09.17,
 *  last modified on 2018-06-13.
 */

class Utils
{
    @SuppressWarnings("deprecation")
    static Spanned fromHtml(String source)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        }
        else
        {
            return Html.fromHtml(source);
        }
    }

}
