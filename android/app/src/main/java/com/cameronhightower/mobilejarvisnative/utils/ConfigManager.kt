package com.cameronhightower.mobilejarvisnative.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import java.io.IOException
import java.util.Properties

/**
 * Singleton manager for configuration values
 */
class ConfigManager private constructor() {
    private val TAG = "ConfigManager"
    private val properties: Properties = Properties()
    private var isInitialized = false
    private lateinit var preferences: SharedPreferences
    
    // Keys for shared preferences
    companion object {
        private const val PREFS_NAME = "MobileJarvisPrefs"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        
        private var instance: ConfigManager? = null

        fun getInstance(): ConfigManager {
            if (instance == null) {
                instance = ConfigManager()
            }
            return instance!!
        }
    }

    /**
     * Initialize the config manager with the application context
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            return
        }

        try {
            val assetManager: AssetManager = context.assets
            val inputStream = assetManager.open("config.properties")
            properties.load(inputStream)
            inputStream.close()
            
            // Initialize shared preferences
            preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            isInitialized = true
            Log.d(TAG, "ConfigManager initialized with ${properties.size} properties")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load config.properties: ${e.message}", e)
            // Create empty properties to prevent null pointer exceptions
            // Still initialize preferences even if config file fails
            preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isInitialized = true
        }
    }

    /**
     * Get the Picovoice access key from config
     */
    fun getPicovoiceAccessKey(): String {
        val key = properties.getProperty("picovoice.accessKey", "")
        if (key.isEmpty()) {
            Log.w(TAG, "Picovoice access key not found in config.properties")
        }
        return key
    }

    /**
     * Get any property from the config file
     */
    fun getProperty(key: String, defaultValue: String = ""): String {
        return properties.getProperty(key, defaultValue)
    }
    
    /**
     * Save wake word enabled preference
     */
    fun setWakeWordEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        Log.d(TAG, "Saved wake word enabled preference: $enabled")
    }
    
    /**
     * Get wake word enabled preference
     */
    fun isWakeWordEnabled(): Boolean {
        val enabled = preferences.getBoolean(KEY_WAKE_WORD_ENABLED, false)
        Log.d(TAG, "Retrieved wake word enabled preference: $enabled")
        return enabled
    }
} 