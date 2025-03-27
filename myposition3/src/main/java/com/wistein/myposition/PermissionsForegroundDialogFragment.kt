package com.wistein.myposition

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/*****************************************************************************
 * PermissionsForegroundDialogFragment provides the permission handling, which
 * is necessary since Android Marshmallow (M) for
 * - ACCESS_COARSE_LOCATION,
 * - ACCESS_FINE_LOCATION
 *
 * Based on RuntimePermissionsExample-master created by tylerjroach on 8/31/16,
 * licensed under the MIT License.
 *
 * Adopted for MyPosition3 by wistein on 2019-02-08,
 * last edited in java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2025-02-22
 */
class PermissionsForegroundDialogFragment : DialogFragment() {
    private var context: Context? = null

    private var externalGrantNeeded = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "43, onCreate")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Request foreground location permission
        requestForegroundPermission()
    }
    // End of onCreate()

    // Request foreground location permission with single permissions launcher
    private fun requestForegroundPermission() {
        if (MyDebug.DLOG) Log.i(TAG, "55, requestForegroundPermissions")
        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        permissionLauncherForeground.launch(permission)
    }

    // Request foreground location permissions in system settings dialog
    private val permissionLauncherForeground = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            externalGrantNeeded = false
            if (MyDebug.DLOG) Log.i(TAG, "70, permissionLauncherForeground granted: $isGranted")
            dismiss()
        } else {
            externalGrantNeeded = true
            showAppSettingsForegroundDialog()
        }
    }

    // Inform about missing necessary foreground permissions and show settings
    private fun showAppSettingsForegroundDialog() {
        if (MyDebug.DLOG) Log.i(TAG, "80, AppSettingsForegroundDialog")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_fine_location_title))
            .setMessage(getString(R.string.dialog_fine_location_message))
            .setPositiveButton(getString(R.string.app_settings))
            { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", "com.wistein.myposition", null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel))
            { _: DialogInterface?, _: Int ->
                dismiss()
            }
            .create().show()
    }

    override fun onDetach() {
        super.onDetach()

        if (MyDebug.DLOG) Log.i(TAG, "103, onDetach")

        context = null
    }

    companion object {
        private const val TAG = "PermForegrDlgFragm"

        @JvmStatic
        fun newInstance(): PermissionsForegroundDialogFragment {
            return PermissionsForegroundDialogFragment()
        }
    }

}
