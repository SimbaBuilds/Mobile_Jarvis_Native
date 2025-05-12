package com.anonymous.MobileJarvisNative.settings

import android.util.Log
import com.anonymous.MobileJarvisNative.ConfigManager
import com.facebook.react.bridge.*

/**
 * Bridge module for exposing app settings to React Native
 */
class SettingsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val TAG = "SettingsModule"
    private val configManager = ConfigManager.getInstance()
    
    override fun getName(): String {
        return "SettingsModule"
    }
    
    /**
     * Get server API configuration
     */
    @ReactMethod
    fun getServerApiConfig(promise: Promise) {
        try {
            val result = Arguments.createMap()
            result.putString("baseUrl", configManager.getServerApiBaseUrl())
            result.putString("apiEndpoint", configManager.getServerApiEndpoint())
            
            Log.d(TAG, "Sending server API config to JS: ${result.toString()}")
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting server API config", e)
            promise.reject("ERR_SETTINGS", e.message, e)
        }
    }
    
    /**
     * Update server API configuration
     */
    @ReactMethod
    fun updateServerApiConfig(baseUrl: String, apiEndpoint: String, promise: Promise) {
        try {
            // Update the values in shared preferences
            val prefs = reactContext.getSharedPreferences("config_prefs", ReactApplicationContext.MODE_PRIVATE)
            val editor = prefs.edit()
            
            if (baseUrl.isNotEmpty()) {
                editor.putString("server_api_base_url", baseUrl)
            }
            
            if (apiEndpoint.isNotEmpty()) {
                editor.putString("server_api_endpoint", apiEndpoint)
            }
            
            editor.apply()
            
            Log.d(TAG, "Updated server API config - baseUrl: $baseUrl, apiEndpoint: $apiEndpoint")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating server API config", e)
            promise.reject("ERR_SETTINGS_UPDATE", e.message, e)
        }
    }
    
    /**
     * Get full application configuration
     */
    @ReactMethod
    fun getAppConfig(promise: Promise) {
        try {
            val result = Arguments.createMap()
            
            // Server API config
            val serverConfig = Arguments.createMap()
            serverConfig.putString("baseUrl", configManager.getServerApiBaseUrl())
            serverConfig.putString("apiEndpoint", configManager.getServerApiEndpoint())
            result.putMap("serverApi", serverConfig)
            
            // API keys (you might want to handle these more securely)
            val apiKeys = Arguments.createMap()
            apiKeys.putString("picovoice", configManager.getPicovoiceAccessKey())
            apiKeys.putString("openai", configManager.getProperty("openai_api_key"))
            apiKeys.putString("deepgram", configManager.getDeepgramApiKey())
            apiKeys.putString("elevenlabs", configManager.getElevenLabsApiKey())
            result.putMap("apiKeys", apiKeys)
            
            Log.d(TAG, "Sending app config to JS")
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app config", e)
            promise.reject("ERR_APP_CONFIG", e.message, e)
        }
    }
} 