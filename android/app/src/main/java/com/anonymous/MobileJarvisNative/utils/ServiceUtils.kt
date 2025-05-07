package com.anonymous.MobileJarvisNative.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager

/**
 * Utility functions for service operations
 */
object ServiceUtils {
    private const val TAG = "ServiceUtils"
    
    /**
     * Start a foreground service
     * 
     * @param context Application context
     * @param serviceClass The service class to start
     * @return Boolean indicating if service was started successfully
     */
    fun startForegroundService(context: Context, serviceClass: Class<*>): Boolean {
        return try {
            Log.i(TAG, "Starting service: ${serviceClass.simpleName}")
            val serviceIntent = Intent(context, serviceClass)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "Started as foreground service")
            } else {
                context.startService(serviceIntent)
                Log.d(TAG, "Started as normal service")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service: ${serviceClass.simpleName}", e)
            false
        }
    }
    
    /**
     * Stop a service
     * 
     * @param context Application context
     * @param serviceClass The service class to stop
     * @return Boolean indicating if service was stopped successfully
     */
    fun stopService(context: Context, serviceClass: Class<*>): Boolean {
        return try {
            Log.i(TAG, "Stopping service: ${serviceClass.simpleName}")
            val serviceIntent = Intent(context, serviceClass)
            context.stopService(serviceIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service: ${serviceClass.simpleName}", e)
            false
        }
    }
    
    /**
     * Save wake word service state to shared preferences
     * 
     * @param context Application context
     * @param isEnabled Whether the wake word service is enabled
     */
    fun saveWakeWordServiceState(context: Context, isEnabled: Boolean) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putBoolean(Constants.Prefs.WAKE_WORD_ENABLED, isEnabled).apply()
            Log.d(TAG, "Saved wake word state to preferences: $isEnabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving wake word service state", e)
        }
    }
    
    /**
     * Get wake word service state from shared preferences
     * 
     * @param context Application context
     * @return Boolean indicating if wake word service should be enabled
     */
    fun getWakeWordServiceState(context: Context): Boolean {
        return try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.getBoolean(Constants.Prefs.WAKE_WORD_ENABLED, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading wake word service state", e)
            false
        }
    }
} 