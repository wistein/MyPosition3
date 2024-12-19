package com.wistein.myposition

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**********************************************************************
 * PermissionsDialogFragment provides the permission handling, which is
 * necessary since Android Marshmallow (M)
 *
 * Original version from RuntimePermissionsExample-master created by
 * tylerjroach on 8/31/16, licensed under the MIT License.
 *
 * Adopted for MyPosition3 by wistein on 2019-02-08,
 * last edited in java on 2024-09-30,
 * converted to Kotlin on 2024-09-30,
 * last edited on 2024-09-30.
 */
class PermissionsDialogFragment : DialogFragment() {
    private var context: Context? = null
    private var listener: PermissionsGrantedCallback? = null

    private var shouldResolve = false
    private var externalGrantNeeded = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        if (context is PermissionsGrantedCallback) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false
        requestNecessaryPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (shouldResolve) {
            if (externalGrantNeeded) {
                showAppSettingsDialog()
            }
            else {
                //permissions have been accepted
                if (listener != null) {
                    listener!!.permissionCaptureFragment()
                    dismiss()
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        context = null
        listener = null
    }

    // Solution with multiple permissions launcher
    private fun requestNecessaryPermissions() {

        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        //launcher permissions request dialog
        permissionLauncherMultiple.launch(permissions)
    }

    // Request multiple permissions
    private val permissionLauncherMultiple = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    )
    { result ->
        // check if permissions were granted from permission request dialog or already granted before
        var allAreGranted = true
        shouldResolve = true
        for (isGranted in result.values) {
            allAreGranted = allAreGranted && isGranted
        }

        if (allAreGranted) {
            // ok, multiple permissions are granted
            externalGrantNeeded = false
        } else {
            //All or some Permissions were denied so can't do the task that requires that permission
            externalGrantNeeded = true
            Toast.makeText(this.context, R.string.perm_declined, Toast.LENGTH_SHORT).show()
        }
    }

    // Query missing permissions
    private fun showAppSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.perm_required))
            .setMessage(getString(R.string.perm_hint) + " " + getString(R.string.perm_hint1))
            .setPositiveButton(getString(R.string.app_settings))
            { _: DialogInterface?, _: Int ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri =
                    Uri.fromParts("package", requireContext().applicationContext.packageName, null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { _: DialogInterface?, _: Int -> dismiss() }
            .create().show()
    }

    interface PermissionsGrantedCallback {
        fun permissionCaptureFragment()
    }

    companion object {
        @JvmStatic
        fun newInstance(): PermissionsDialogFragment {
            return PermissionsDialogFragment()
        }
    }

}
