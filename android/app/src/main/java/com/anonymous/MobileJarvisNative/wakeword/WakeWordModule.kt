package com.anonymous.MobileJarvisNative.wakeword

import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.anonymous.MobileJarvisNative.MainActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build

class WakeWordModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val TAG = "WakeWordModule"
    private var isServiceRunning = false
    private var wakeWordReceiver: BroadcastReceiver? = null
    
    init {
        // Register broadcast receiver for wake word detection
        registerWakeWordReceiver()
        
        // Log initial state
        val prefs = reactApplicationContext.getSharedPreferences("wakeword_prefs", Context.MODE_PRIVATE)
        val initialState = prefs.getBoolean("wake_word_enabled", false)
        Log.i(TAG, "WakeWordModule initialized with state: enabled=$initialState")
    }
    
    private fun registerWakeWordReceiver() {
        if (wakeWordReceiver == null) {
            wakeWordReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == "com.anonymous.MobileJarvisNative.WAKE_WORD_DETECTED_RN") {
                        Log.d(TAG, "Received wake word broadcast in React Native module")
                        
                        // Send event to JavaScript
                        val params = Arguments.createMap()
                        params.putDouble("timestamp", System.currentTimeMillis().toDouble())
                        sendEvent("wakeWordDetected", params)
                    }
                }
            }
            
            val intentFilter = IntentFilter("com.anonymous.MobileJarvisNative.WAKE_WORD_DETECTED_RN")
            
            // Use the appropriate registration method based on Android version
            // For Android 13+ (API 33+), specify RECEIVER_NOT_EXPORTED to follow best practices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                reactApplicationContext.registerReceiver(wakeWordReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                reactApplicationContext.registerReceiver(wakeWordReceiver, intentFilter)
            }
            
            Log.d(TAG, "Registered wake word broadcast receiver")
        }
    }
    
    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        
        // Unregister broadcast receiver
        if (wakeWordReceiver != null) {
            try {
                reactApplicationContext.unregisterReceiver(wakeWordReceiver)
                wakeWordReceiver = null
                Log.d(TAG, "Unregistered wake word broadcast receiver")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering wake word receiver: ${e.message}", e)
            }
        }
    }
    
    override fun getName(): String {
        return "WakeWordModule"
    }
    
    override fun getConstants(): Map<String, Any> {
        val constants = HashMap<String, Any>()
        constants["WAKE_WORD_DETECTED"] = "wakeWordDetected"
        return constants
    }
    
    @ReactMethod
    fun addListener(eventName: String) {
        // Required for RN event emitter
        Log.d(TAG, "Added listener for $eventName")
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for RN event emitter
        Log.d(TAG, "Removed $count listener(s)")
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
            val prefs = reactApplicationContext.getSharedPreferences("wakeword_prefs", Context.MODE_PRIVATE)
            
            if (isServiceRunning) {
                Log.d(TAG, "Service already running, updating state only")
                prefs.edit().putBoolean("wake_word_enabled", true).apply()
                val result = Arguments.createMap()
                result.putBoolean("success", true)
                result.putString("message", "Service already running")
                promise.resolve(result)
                return
            }

            Log.d(TAG, "Starting wake word service...")
            val context = reactApplicationContext
            val intent = Intent(context, WakeWordService::class.java)
            
            // Set wake word enabled state
            prefs.edit().putBoolean("wake_word_enabled", true).apply()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            promise.reject("START_DETECTION_ERROR", "Failed to start wake word detection: ${e.message}", e)
        }
    }
    
    /**
     * Stop wake word detection service
     */
    @ReactMethod
    fun stopDetection(promise: Promise) {
        try {
            if (!isServiceRunning) {
                val result = Arguments.createMap()
                result.putBoolean("success", true)
                result.putString("message", "Service not running")
                promise.resolve(result)
                return
            }

            val context = reactApplicationContext
            val intent = Intent(context, WakeWordService::class.java)
            
            // Set wake word disabled state
            val prefs = context.getSharedPreferences("wakeword_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("wake_word_enabled", false).apply()
            
            context.stopService(intent)
            isServiceRunning = false
            
            val result = Arguments.createMap()
            result.putBoolean("success", true)
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping wake word detection: ${e.message}", e)
            promise.reject("STOP_DETECTION_ERROR", "Failed to stop wake word detection: ${e.message}", e)
        }
    }
    
    /**
     * Get the current status of wake word detection
     */
    @ReactMethod
    fun getStatus(promise: Promise) {
        try {
            val prefs = reactApplicationContext.getSharedPreferences("wakeword_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("wake_word_enabled", false)
            val serviceRunning = WakeWordService.isRunning()
            
            Log.d(TAG, "Current wake word state: enabled=$enabled, serviceRunning=$serviceRunning")
            
            // If the service is running but enabled is false, or vice versa, fix the inconsistency
            if (enabled != serviceRunning) {
                Log.w(TAG, "Detected state inconsistency, fixing...")
                prefs.edit().putBoolean("wake_word_enabled", serviceRunning).apply()
                val result = Arguments.createMap()
                result.putBoolean("enabled", serviceRunning)
                promise.resolve(result)
                return
            }
            
            val result = Arguments.createMap()
            result.putBoolean("enabled", enabled)
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting wake word status: ${e.message}", e)
            promise.reject("GET_STATUS_ERROR", "Failed to get wake word status: ${e.message}", e)
        }
    }
    
    /**
     * Set the access key for Picovoice
     */
    @ReactMethod
    fun setAccessKey(accessKey: String, promise: Promise) {
        try {
            if (accessKey.isBlank()) {
                promise.reject("INVALID_KEY", "Access key cannot be empty")
                return
            }

            val prefs = reactApplicationContext.getSharedPreferences("picovoice_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("picovoice_access_key", accessKey).apply()
            
            val result = Arguments.createMap()
            result.putBoolean("success", true)
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting access key: ${e.message}", e)
            promise.reject("SET_KEY_ERROR", "Failed to set access key: ${e.message}", e)
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