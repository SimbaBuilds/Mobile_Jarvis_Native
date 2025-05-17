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
    
    /**
     * Get speech recognition minimum length in milliseconds
     */
    fun getSpeechRecognitionMinimumLengthMs(): Int {
        val defaultValue = 4000
        val propertyValue = properties.getProperty(Constants.Config.SPEECH_RECOGNITION_MINIMUM_LENGTH_MS, defaultValue.toString())
        return propertyValue.toIntOrNull() ?: defaultValue
    }
    
    /**
     * Get speech recognition complete silence length in milliseconds
     */
    fun getSpeechRecognitionCompleteSilenceMs(): Int {
        val defaultValue = 3500
        val propertyValue = properties.getProperty(Constants.Config.SPEECH_RECOGNITION_COMPLETE_SILENCE_MS, defaultValue.toString())
        return propertyValue.toIntOrNull() ?: defaultValue
    }
    
    /**
     * Get speech recognition possible silence length in milliseconds
     */
    fun getSpeechRecognitionPossibleSilenceMs(): Int {
        val defaultValue = 2500
        val propertyValue = properties.getProperty(Constants.Config.SPEECH_RECOGNITION_POSSIBLE_SILENCE_MS, defaultValue.toString())
        return propertyValue.toIntOrNull() ?: defaultValue
    }
    
    /**
     * Get speech retry delay in milliseconds
     */
    fun getSpeechRetryDelayMs(): Int {
        val defaultValue = 800
        val propertyValue = properties.getProperty(Constants.Config.SPEECH_RETRY_DELAY_MS, defaultValue.toString())
        return propertyValue.toIntOrNull() ?: defaultValue
    }
    
    /**
     * Get speech final message delay in milliseconds
     */
    fun getSpeechFinalMessageDelayMs(): Int {
        val defaultValue = 2500
        val propertyValue = properties.getProperty(Constants.Config.SPEECH_FINAL_MESSAGE_DELAY_MS, defaultValue.toString())
        return propertyValue.toIntOrNull() ?: defaultValue
    }
    
    /**
     * Get max no speech retries
     */
    fun getMaxNoSpeechRetries(): Int {
        val defaultValue = 2
        val propertyValue = properties.getProperty(Constants.Config.SPEECH_MAX_NO_SPEECH_RETRIES, defaultValue.toString())
        return propertyValue.toIntOrNull() ?: defaultValue
    }
    
    /**
     * Check if custom recognizer parameters should be used
     */
    fun useCustomRecognizerParams(): Boolean {
        val defaultValue = false
        val propertyValue = properties.getProperty(Constants.Config.USE_CUSTOM_RECOGNIZER_PARAMS, defaultValue.toString())
        return propertyValue.toBoolean()
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