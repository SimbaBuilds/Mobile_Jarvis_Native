package com.anonymous.MobileJarvisNative.permissions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.anonymous.MobileJarvisNative.utils.PermissionUtils

/**
 * React Native module for handling permissions
 */
class PermissionsModule(private val reactContext: ReactApplicationContext) 
    : ReactContextBaseJavaModule(reactContext), ActivityEventListener {
    
    private val TAG = "PermissionsModule"
    private val PERMISSION_AUDIO_REQUEST_CODE = 1001
    
    init {
        reactContext.addActivityEventListener(this)
    }
    
    override fun getName(): String {
        return "PermissionsModule"
    }
    
    /**
     * Check if the app has audio permission
     */
    @ReactMethod
    fun checkAudioPermission(promise: Promise) {
        val hasPermission = PermissionUtils.hasAudioPermission(reactContext)
        promise.resolve(hasPermission)
    }
    
    /**
     * Request audio permission
     */
    @ReactMethod
    fun requestAudioPermission(promise: Promise) {
        val currentActivity = currentActivity
        
        if (currentActivity == null) {
            Log.e(TAG, "No activity available for permission request")
            promise.reject("ERR_PERMISSION", "No activity available for permission request")
            return
        }
        
        if (PermissionUtils.hasAudioPermission(reactContext)) {
            // Already have permission
            promise.resolve(true)
            return
        }
        
        try {
            ActivityCompat.requestPermissions(
                currentActivity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_AUDIO_REQUEST_CODE
            )
            // The result will be handled in onActivityResult
            // We'll store the promise to resolve later
            pendingPermissionPromise = promise
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permissions", e)
            promise.reject("ERR_PERMISSION", e.message, e)
        }
    }
    
    /**
     * Check if the app is exempt from battery optimization
     */
    @ReactMethod
    fun isBatteryOptimizationExempt(promise: Promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Battery optimization setting was introduced in Android M
            promise.resolve(true)
            return
        }
        
        try {
            val powerManager = reactContext.getSystemService(Activity.POWER_SERVICE) as PowerManager
            val packageName = reactContext.packageName
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
            promise.resolve(isIgnoringBatteryOptimizations)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization state", e)
            promise.reject("ERR_BATTERY_CHECK", e.message, e)
        }
    }
    
    /**
     * Request battery optimization exemption
     */
    @ReactMethod
    fun requestBatteryOptimizationExemption(promise: Promise) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            promise.resolve(true)
            return
        }
        
        val currentActivity = currentActivity
        if (currentActivity == null) {
            promise.reject("ERR_ACTIVITY_NOT_FOUND", "No activity available")
            return
        }
        
        try {
            val powerManager = reactContext.getSystemService(Activity.POWER_SERVICE) as PowerManager
            val packageName = reactContext.packageName
            
            if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // Already exempted
                promise.resolve(true)
                return
            }
            
            // Request the user to disable battery optimization for this app
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            currentActivity.startActivity(intent)
            
            // Need to check the result later since this is a system dialog
            // For now, just resolve the promise
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting battery optimization exemption", e)
            promise.reject("ERR_BATTERY_EXEMPTION", e.message, e)
        }
    }
    
    /**
     * Add an event listener
     */
    @ReactMethod
    fun addListener(eventName: String) {
        // Required for RN built in Event Emitter Calls.
    }
    
    /**
     * Remove event listeners
     */
    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for RN built in Event Emitter Calls.
    }
    
    /**
     * Handle permission results
     */
    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        // Not used in this module
    }
    
    override fun onNewIntent(intent: Intent?) {
        // Not used in this module
    }
    
    /**
     * Send an event to JavaScript
     */
    private fun sendEvent(eventName: String, params: Map<String, Any>) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
    
    /**
     * Handle permission request results
     */
    private var pendingPermissionPromise: Promise? = null
    
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_AUDIO_REQUEST_CODE && permissions.isNotEmpty()) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            
            // Emit event to JS listeners
            sendEvent("onPermissionResult", mapOf(
                "permission" to "audio",
                "granted" to granted
            ))
            
            // Resolve the pending promise
            pendingPermissionPromise?.resolve(granted)
            pendingPermissionPromise = null
        }
    }
} 