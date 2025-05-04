package com.cameronhightower.mobilejarvisnative.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cameronhightower.mobilejarvisnative.utils.Constants.RequestCodes

/**
 * Utility functions for handling permissions
 */
object PermissionUtils {
    private const val TAG = "PermissionUtils"
    
    /**
     * Check if the app has the required permission
     * 
     * @param context Application context
     * @param permission The permission to check
     * @return Boolean indicating if the permission is granted
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if audio recording permission is granted, and request if not
     * 
     * @param activity The activity to request permissions from
     * @return Boolean indicating if audio permission is granted
     */
    fun checkAudioPermission(activity: Activity): Boolean {
        try {
            Log.d(TAG, "Checking audio permissions")
            if (!hasPermission(activity, Manifest.permission.RECORD_AUDIO)) {
                Log.i(TAG, "Audio permission not granted, requesting permission")
                requestAudioPermission(activity)
                return false
            } else {
                Log.i(TAG, "Audio permission already granted")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            UIUtils.showToast(activity, "Error checking permissions: ${e.message}", Toast.LENGTH_SHORT)
            return false
        }
    }
    
    /**
     * Request audio recording permission
     * 
     * @param activity The activity to request permissions from
     * @param requestCode The request code to use for the permission request
     */
    fun requestAudioPermission(activity: Activity, requestCode: Int = RequestCodes.PERMISSIONS) {
        try {
            Log.d(TAG, "Requesting audio permission")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio permission", e)
        }
    }
    
    /**
     * Handle permission request results
     * 
     * @param context Activity context
     * @param requestCode Request code from onRequestPermissionsResult
     * @param permissions Permissions array from onRequestPermissionsResult
     * @param grantResults Grant results array from onRequestPermissionsResult
     */
    fun handlePermissionResult(
        context: Context,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        try {
            if (requestCode == RequestCodes.PERMISSIONS) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Audio permission granted")
                    UIUtils.showToast(context, "Permission granted", Toast.LENGTH_SHORT)
                } else {
                    Log.e(TAG, "Audio permission denied")
                    UIUtils.showToast(context, "Permission denied", Toast.LENGTH_SHORT)
                    
                    // Show rationale for why we need this permission
                    if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, 
                            Manifest.permission.RECORD_AUDIO)) {
                        AlertDialog.Builder(context)
                            .setTitle("Microphone Permission Required")
                            .setMessage("This app needs microphone access to detect voice commands. " +
                                    "Without this permission, voice assistant functionality won't work.")
                            .setPositiveButton("Grant Permission") { _, _ ->
                                requestAudioPermission(context)
                            }
                            .setNegativeButton("Cancel") { _, _ ->
                                UIUtils.showToast(context, "Voice functionality limited without microphone permission", 
                                    Toast.LENGTH_LONG)
                            }
                            .show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in permission result handling", e)
        }
    }
    
    /**
     * Check if app is exempt from battery optimizations, and request exemption if not
     * 
     * @param activity The activity to show dialogs and request exemption
     */
    fun checkAndRequestBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
                val packageName = activity.packageName
                
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    Log.i(TAG, "App is not exempt from battery optimization, showing dialog")
                    
                    // Show dialog explaining why we need this permission
                    AlertDialog.Builder(activity)
                        .setTitle("Battery Optimization")
                        .setMessage("For reliable wake word detection, this app needs to be exempt from battery optimizations. This will allow the app to run properly in the background.")
                        .setPositiveButton("Allow") { _, _ ->
                            // Request battery optimization exemption
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                                activity.startActivity(intent)
                                Log.d(TAG, "Battery optimization exemption request started")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error requesting battery optimization exemption", e)
                            }
                        }
                        .setNegativeButton("Later") { _, _ ->
                            Log.d(TAG, "User declined battery optimization exemption")
                        }
                        .create()
                        .show()
                } else {
                    Log.d(TAG, "App is already exempt from battery optimization")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking battery optimization status", e)
            }
        }
    }
    
    /**
     * Check if the app is exempt from battery optimizations
     * 
     * @param context Application context
     * @return Boolean indicating if the app is exempt from battery optimizations
     */
    fun isBatteryOptimizationExempt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true // On older Android versions, this is not an issue
    }
}
