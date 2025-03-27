package com.wistein.myposition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/*****************************************************************
 * Dummy activity forces the app to reenter MyPositionActivity for
 * an instant location request
 *
 * Created and copyright by wmstein on 2019-08-12,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2025-02-21.
 */
class DummyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exit()
    }

    private fun exit() {
        super.finish()
    }
}
