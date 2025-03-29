package com.wistein.myposition

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.text.util.Linkify
import android.widget.TextView
import com.wistein.myposition.Utils.fromHtml
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

/***********************************************************************
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * HelpDialog.java
 * Custom class for displaying the Help Dialog
 *
 * Adopted 2019 by wistein for MyPosition3,
 * last edited in Java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2025-03-29
 */
class HelpDialog : Activity() {
    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = MyPosition.getPrefs()
        val screenOrientL = prefs.getBoolean("screen_Orientation", false)
        val darkScreen = prefs.getBoolean("dark_Screen", false)

        requestedOrientation = if (screenOrientL) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (darkScreen)
        {
            setTheme(R.style.AppTheme_Dark)
        }
        else
        {
            setTheme(R.style.AppTheme_Light)
        }

        val dialog = intent.getStringExtra("dialog")

        val language = Locale.getDefault().toString().substring(0, 2)

        setContentView(R.layout.activity_dialog)
        val tvHead = findViewById<TextView>(R.id.help_head)
        val tvText = findViewById<TextView>(R.id.help_text)
        if (dialog == "help") {
            if (language == "de") {
                tvHead.text = fromHtml(readRawTextFile(R.raw.help_head_de, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.help_de, this))
            } else {
                tvHead.text = fromHtml(readRawTextFile(R.raw.help_head, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.help, this))
            }
            tvHead.setLinkTextColor(Color.BLUE)
            Linkify.addLinks(tvHead, Linkify.WEB_URLS)
            tvText.setLinkTextColor(Color.BLUE)
            Linkify.addLinks(tvText, Linkify.WEB_URLS)
        }
        else if (dialog == "about") {
            if (language == "de") {
                tvHead.text = fromHtml(readRawTextFile(R.raw.info_head_de, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.info_de, this))
            } else {
                tvHead.text = fromHtml(readRawTextFile(R.raw.info_head, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.info, this))
            }
            tvHead.setLinkTextColor(Color.BLUE)
            Linkify.addLinks(tvHead, Linkify.WEB_URLS)
            tvText.setLinkTextColor(Color.BLUE)
            Linkify.addLinks(tvText, Linkify.WEB_URLS)
        }
    }

    companion object {
        private fun readRawTextFile(id: Int, context: Context): String? {
            val inputStream = context.resources.openRawResource(id)
            val `in` = InputStreamReader(inputStream)
            val buf = BufferedReader(`in`)
            var line: String?
            val text = StringBuilder()
            try {
                while ((buf.readLine().also { line = it }) != null) text.append(line)
            } catch (_: IOException) {
                return null
            }
            return text.toString()
        }
    }

}
