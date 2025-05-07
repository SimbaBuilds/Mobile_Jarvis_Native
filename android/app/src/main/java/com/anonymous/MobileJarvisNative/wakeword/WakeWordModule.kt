@ -1,91 +0,0 @@
package com.anonymous.MobileJarvisNative.wakeword

import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.anonymous.MobileJarvisNative.MainActivity

class WakeWordModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val TAG = "WakeWordModule"
    private var isServiceRunning = false
    
    override fun getName(): String {
        return "WakeWordModule"
    }
    
    /**
     * Check if wake word detection is available on this device
     */
    @ReactMethod
    fun isAvailable(promise: Promise) {
        val result = Arguments.createMap()
        
        // For now, simply check if we're on Android since that's the only platform we support
        result.putBoolean("available", true)
        promise.resolve(result)
    }
    
    /**
     * Start wake word detection service
     */
    @ReactMethod
    fun startDetection(promise: Promise) {
        try {
            val context = reactApplicationContext
            val intent = Intent(context, WakeWordService::class.java)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            isServiceRunning = true
            
            val result = Arguments.createMap()
            result.putBoolean("success", true)
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word detection: ${e.message}", e)
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("error", "Failed to start wake word detection: ${e.message}")
            promise.resolve(result)
        }
    }
    
    /**
     * Stop wake word detection service
     */
    @ReactMethod
    fun stopDetection(promise: Promise) {
        try {
            val context = reactApplicationContext
            val intent = Intent(context, WakeWordService::class.java)
            context.stopService(intent)
            
            isServiceRunning = false
            
            val result = Arguments.createMap()
            result.putBoolean("success", true)
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping wake word detection: ${e.message}", e)
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("error", "Failed to stop wake word detection: ${e.message}")
            promise.resolve(result)
        }
    }
    
    /**
     * Get the current status of wake word detection
     */
    @ReactMethod
    fun getStatus(promise: Promise) {
        try {
            val prefs = reactApplicationContext.getSharedPreferences("wakeword_prefs", android.content.Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("wake_word_enabled", false)
            
            val result = Arguments.createMap()
            result.putBoolean("enabled", enabled)
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting wake word status: ${e.message}", e)
            val result = Arguments.createMap()
            result.putBoolean("enabled", false)
            promise.resolve(result)
        }
    }
    
    /**
     * Set the access key for Picovoice
     */
    @ReactMethod
    fun setAccessKey(accessKey: String, promise: Promise) {
        try {
            val prefs = reactApplicationContext.getSharedPreferences("picovoice_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("picovoice_access_key", accessKey).apply()
            
            val result = Arguments.createMap()
            result.putBoolean("success", true)
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting access key: ${e.message}", e)
            val result = Arguments.createMap()
            result.putBoolean("success", false)
            result.putString("error", "Failed to set access key: ${e.message}")
            promise.resolve(result)
        }
    }
    
    /**
     * Helper method to send events to JavaScript
     */
    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
} 