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
     * Get the Picovoice Access Key
     */
    fun getPicovoiceAccessKey(): String {
        val propertyValue = properties.getProperty(Constants.Config.PICOVOICE_ACCESS_KEY, defaultEmptyKey)
        return prefs.getString(Constants.Config.PICOVOICE_ACCESS_KEY, propertyValue) ?: propertyValue
    }
    
    /**
     * Get the Deepgram API Key
     */
    fun getDeepgramApiKey(): String {
        val propertyValue = properties.getProperty(Constants.Config.DEEPGRAM_API_KEY, defaultEmptyKey)
        return prefs.getString(Constants.Config.DEEPGRAM_API_KEY, propertyValue) ?: propertyValue
    }
    
    /**
     * Get the ElevenLabs API Key
     */
    fun getElevenLabsApiKey(): String {
        val propertyValue = properties.getProperty(Constants.Config.ELEVENLABS_API_KEY, defaultEmptyKey)
        return prefs.getString(Constants.Config.ELEVENLABS_API_KEY, propertyValue) ?: propertyValue
    }
    
    /**
     * Get the server API base URL
     */
    fun getServerApiBaseUrl(): String {
        val propertyValue = properties.getProperty(Constants.Config.SERVER_API_BASE_URL, "http://192.168.1.131:8000")
        return prefs.getString(Constants.Config.SERVER_API_BASE_URL, propertyValue) ?: propertyValue
    }
    
    /**
     * Get the server API endpoint
     */
    fun getServerApiEndpoint(): String {
        val propertyValue = properties.getProperty(Constants.Config.SERVER_API_ENDPOINT, "/api/chat")
        return prefs.getString(Constants.Config.SERVER_API_ENDPOINT, propertyValue) ?: propertyValue
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