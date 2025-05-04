package com.cameronhightower.mobilejarvisnative.modules.permissions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.cameronhightower.mobilejarvisnative.utils.Constants
import com.cameronhightower.mobilejarvisnative.utils.PermissionUtils
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener

class PermissionsModule(reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext), PermissionListener {
    
    private val TAG = "PermissionsModule"
    private var permissionCallback: Promise? = null
    
    override fun getName(): String {
        return "PermissionsModule"
    }
    
    /**
     * Emits events to JavaScript
     */
    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
    
    /**
     * Check if the app has audio recording permission
     */
    @ReactMethod
    fun checkAudioPermission(promise: Promise) {
        try {
            val currentActivity = currentActivity
            if (currentActivity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            val hasPermission = PermissionUtils.hasPermission(
                currentActivity,
                Manifest.permission.RECORD_AUDIO
            )
            
            promise.resolve(hasPermission)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking audio permission", e)
            promise.reject("PERMISSION_ERROR", e.message, e)
        }
    }
    
    /**
     * Request audio recording permission
     */
    @ReactMethod
    fun requestAudioPermission(promise: Promise) {
        try {
            val activity = currentActivity as? PermissionAwareActivity
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            permissionCallback = promise
            
            val hasPermission = PermissionUtils.hasPermission(
                reactApplicationContext,
                Manifest.permission.RECORD_AUDIO
            )
            
            if (hasPermission) {
                promise.resolve(true)
                return
            }
            
            activity.requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                Constants.RequestCodes.AUDIO_PERMISSION,
                this
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio permission", e)
            promise.reject("PERMISSION_ERROR", e.message, e)
        }
    }
    
    /**
     * Check if app is exempt from battery optimizations
     */
    @ReactMethod
    fun isBatteryOptimizationExempt(promise: Promise) {
        try {
            val isExempt = PermissionUtils.isBatteryOptimizationExempt(reactApplicationContext)
            promise.resolve(isExempt)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization", e)
            promise.reject("BATTERY_OPT_ERROR", e.message, e)
        }
    }
    
    /**
     * Request exemption from battery optimizations
     */
    @ReactMethod
    fun requestBatteryOptimizationExemption(promise: Promise) {
        try {
            val activity = currentActivity
            if (activity == null) {
                promise.reject("NO_ACTIVITY", "No activity available")
                return
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (PermissionUtils.isBatteryOptimizationExempt(reactApplicationContext)) {
                    promise.resolve(true)
                    return
                }
                
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${reactApplicationContext.packageName}")
                }
                
                activity.startActivityForResult(
                    intent,
                    Constants.RequestCodes.BATTERY_OPTIMIZATION,
                    null
                )
                promise.resolve(true)
            } else {
                // Not applicable for Android versions below M
                promise.resolve(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting battery optimization exemption", e)
            promise.reject("BATTERY_OPT_ERROR", e.message, e)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == Constants.RequestCodes.AUDIO_PERMISSION) {
            val isGranted = grantResults.isNotEmpty() && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            
            permissionCallback?.resolve(isGranted)
            permissionCallback = null
            
            // Also emit an event for listeners
            val params = Arguments.createMap().apply {
                putBoolean("isGranted", isGranted)
                putString("permission", Manifest.permission.RECORD_AUDIO)
            }
            sendEvent("onPermissionResult", params)
            
            return true
        }
        return false
    }
    
    /**
     * Add event listener (for JavaScript)
     */
    @ReactMethod
    fun addListener(eventName: String) {
        // Keep for React Native event emitter
    }
    
    /**
     * Remove event listener (for JavaScript)
     */
    @ReactMethod
    fun removeListeners(count: Int) {
        // Keep for React Native event emitter
    }
}
