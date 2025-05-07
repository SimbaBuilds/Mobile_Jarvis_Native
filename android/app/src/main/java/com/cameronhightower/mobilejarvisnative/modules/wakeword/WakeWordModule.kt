package com.cameronhightower.mobilejarvisnative.modules.wakeword

import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.cameronhightower.mobilejarvisnative.MainActivity

class WakeWordModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val TAG = "WakeWordModule"
    private var isServiceRunning = false
    
    override fun getName(): String {
        return "WakeWordModule"
    }
    
    @ReactMethod
    fun startWakeWordDetection(promise: Promise) {
        try {
            val context = reactApplicationContext
            val intent = Intent(context, WakeWordService::class.java)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            isServiceRunning = true
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word detection: ${e.message}", e)
            promise.reject("ERROR", "Failed to start wake word detection: ${e.message}")
        }
    }
    
    @ReactMethod
    fun stopWakeWordDetection(promise: Promise) {
        try {
            val context = reactApplicationContext
            val intent = Intent(context, WakeWordService::class.java)
            context.stopService(intent)
            
            isServiceRunning = false
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping wake word detection: ${e.message}", e)
            promise.reject("ERROR", "Failed to stop wake word detection: ${e.message}")
        }
    }
    
    @ReactMethod
    fun isWakeWordDetectionRunning(promise: Promise) {
        promise.resolve(isServiceRunning)
    }
    
    @ReactMethod
    fun setWakeWordEnabled(enabled: Boolean, promise: Promise) {
        try {
            val prefs = reactApplicationContext.getSharedPreferences("wakeword_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("wake_word_enabled", enabled).apply()
            
            if (enabled) {
                startWakeWordDetection(promise)
            } else {
                stopWakeWordDetection(promise)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting wake word enabled state: ${e.message}", e)
            promise.reject("ERROR", "Failed to set wake word enabled state: ${e.message}")
        }
    }
    
    @ReactMethod
    fun isWakeWordEnabled(promise: Promise) {
        try {
            val prefs = reactApplicationContext.getSharedPreferences("wakeword_prefs", android.content.Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("wake_word_enabled", false)
            promise.resolve(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting wake word enabled state: ${e.message}", e)
            promise.reject("ERROR", "Failed to get wake word enabled state: ${e.message}")
        }
    }
    
    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
} 