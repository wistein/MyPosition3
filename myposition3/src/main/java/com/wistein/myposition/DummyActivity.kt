package com.wistein.myposition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/*******************************************************************
 * Dummy activity to reenter MyPositionActivity for location request
 * Created and copyright by wmstein on 2019-08-12,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2024-09-30.
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
