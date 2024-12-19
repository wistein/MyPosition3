package com.wistein.myposition

import android.text.Html
import android.text.Spanned

/******************************************************
 * Class Utils cares for Android versions compatibility
 * - in AboutDialog fromHtml
 *
 * Created by wistein on 2017-09-25,
 * last modified in Java on 2018-06-13,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2024-11-19.
 */
internal object Utils {
    @JvmStatic
    fun fromHtml(source: String?): Spanned {
        return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
    }

}
