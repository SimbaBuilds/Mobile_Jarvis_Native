package com.anonymous.MobileJarvisNative

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.anonymous.MobileJarvisNative.utils.Constants
import java.util.Properties

/**
 * Manages configuration settings for the application
 */
class ConfigManager private constructor(private val context: Context) {
    private val TAG = "ConfigManager"
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val properties = Properties()
    
    // Default empty values in case properties file is missing or keys not found
    private val defaultEmptyKey = ""
    
    init {
        loadPropertiesFromAssets()
    }
    
    /**
     * Load properties from config.properties file in assets folder
     */
    private fun loadPropertiesFromAssets() {
        try {
            val inputStream = context.assets.open("config.properties")
            properties.load(inputStream)
            inputStream.close()
            Log.d(TAG, "Successfully loaded properties from config.properties")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading properties from config.properties: ${e.message}", e)
        }
    }
    
    /**
     * Get the Vapi API key
     */
    fun getVapiApiKey(): String {
        val propertyValue = properties.getProperty(Constants.Config.VAPI_API_KEY, defaultEmptyKey)
        return prefs.getString(Constants.Config.VAPI_API_KEY, propertyValue) ?: propertyValue
    }
    
    /**
     * Get the Vapi Assistant ID
     */
    fun getVapiAssistantId(): String {
        val propertyValue = properties.getProperty(Constants.Config.VAPI_ASSISTANT_ID, defaultEmptyKey)
        return prefs.getString(Constants.Config.VAPI_ASSISTANT_ID, propertyValue) ?: propertyValue
    }
    
    /**
     * Get the Picovoice Access Key
     */
    fun getPicovoiceAccessKey(): String {
        val propertyValue = properties.getProperty(Constants.Config.PICOVOICE_ACCESS_KEY, defaultEmptyKey)
        return prefs.getString(Constants.Config.PICOVOICE_ACCESS_KEY, propertyValue) ?: propertyValue
    }
    
    /**
     * Get a property value from config.properties
     */
    fun getProperty(key: String, defaultValue: String = ""): String {
        return properties.getProperty(key, defaultValue)
    }
    
    companion object {
        private var instance: ConfigManager? = null
        
        /**
         * Initialize the ConfigManager
         */
        fun init(context: Context) {
            if (instance == null) {
                instance = ConfigManager(context.applicationContext)
                Log.d("ConfigManager", "Initialized ConfigManager instance")
            }
        }
        
        /**
         * Get the singleton instance of ConfigManager
         */
        fun getInstance(): ConfigManager {
            return instance ?: throw IllegalStateException(
                "ConfigManager not initialized. Call init() first."
            )
        }
    }
} 