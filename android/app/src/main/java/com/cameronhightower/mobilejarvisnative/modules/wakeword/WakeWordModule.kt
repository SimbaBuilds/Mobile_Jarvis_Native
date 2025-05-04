package com.cameronhightower.mobilejarvisnative.modules.wakeword

import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cameronhightower.mobilejarvisnative.utils.PermissionUtils
import com.cameronhightower.mobilejarvisnative.MainActivity
import com.cameronhightower.mobilejarvisnative.utils.ConfigManager
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule

/**
 * React Native module for wake word detection
 */
class WakeWordModule(private val reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext) {
    
    private val TAG = "WakeWordModule"
    
    override fun getName(): String = "WakeWordModule"
    
    /**
     * Get if wake word detection is available
     */
    @ReactMethod
    fun isAvailable(promise: Promise) {
        try {
            val isWakeWordAvailable = checkWakeWordAvailability()
            val response: WritableMap = Arguments.createMap()
            response.putBoolean("available", isWakeWordAvailable)
            
            if (!isWakeWordAvailable) {
                response.putString("reason", "Device does not support wake word detection")
            }
            
            promise.resolve(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking wake word availability: ${e.message}", e)
            promise.reject("ERROR", "Failed to check wake word availability: ${e.message}")
        }
    }
    
    /**
     * Start wake word detection service
     */
    @ReactMethod
    fun startDetection(promise: Promise) {
        try {
            // Check if we have microphone permission
            if (!PermissionUtils.hasPermission(reactContext, Manifest.permission.RECORD_AUDIO)) {
                val response: WritableMap = Arguments.createMap()
                response.putBoolean("success", false)
                response.putString("error", "Microphone permission required")
                promise.resolve(response)
                return
            }
            
            // Check if we have a valid access key
            val accessKey = ConfigManager.getInstance().getPicovoiceAccessKey()
            if (accessKey.isEmpty()) {
                val response: WritableMap = Arguments.createMap()
                response.putBoolean("success", false)
                response.putString("error", "Missing Picovoice access key")
                promise.resolve(response)
                return
            }
            
            // Start the wake word service
            val serviceIntent = Intent(reactContext, WakeWordService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                reactContext.startForegroundService(serviceIntent)
            } else {
                reactContext.startService(serviceIntent)
            }
            
            val response: WritableMap = Arguments.createMap()
            response.putBoolean("success", true)
            promise.resolve(response)
            
            Log.i(TAG, "Wake word detection service started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word detection: ${e.message}", e)
            promise.reject("ERROR", "Failed to start wake word detection: ${e.message}")
        }
    }
    
    /**
     * Stop wake word detection service
     */
    @ReactMethod
    fun stopDetection(promise: Promise) {
        try {
            val serviceIntent = Intent(reactContext, WakeWordService::class.java)
            val stopped = reactContext.stopService(serviceIntent)
            
            val response: WritableMap = Arguments.createMap()
            response.putBoolean("success", stopped)
            
            if (!stopped) {
                response.putString("warning", "Service was not running or could not be stopped")
            }
            
            promise.resolve(response)
            Log.i(TAG, "Wake word detection service stopped: $stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping wake word detection: ${e.message}", e)
            promise.reject("ERROR", "Failed to stop wake word detection: ${e.message}")
        }
    }
    
    /**
     * Set the Picovoice access key
     */
    @ReactMethod
    fun setAccessKey(accessKey: String, promise: Promise) {
        // This would need an actual implementation to save to a secure storage
        // For demonstration purposes, we'll just log it
        Log.i(TAG, "Access key would be stored securely (length: ${accessKey.length})")
        
        val response: WritableMap = Arguments.createMap()
        response.putBoolean("success", true)
        promise.resolve(response)
    }
    
    /**
     * Emit an event to the JS side
     */
    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
    
    /**
     * Check if wake word detection is available on this device
     */
    private fun checkWakeWordAvailability(): Boolean {
        return try {
            // Try to access a Porcupine class to see if libraries are available
            val keywordValue = ai.picovoice.porcupine.Porcupine.BuiltInKeyword.JARVIS.name
            Log.d(TAG, "Porcupine library check passed: $keywordValue")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Wake word detection not available: ${e.message}", e)
            false
        }
    }
}
