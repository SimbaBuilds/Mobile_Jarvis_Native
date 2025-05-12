package com.anonymous.MobileJarvisNative.voice

import android.content.Context
import android.util.Log
import com.anonymous.MobileJarvisNative.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for OpenAI Whisper API for speech-to-text conversion
 */
class WhisperClient(private val context: Context) {
    private val TAG = "WhisperClient"
    private var isInitialized = false
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var configManager: ConfigManager
    
    /**
     * Initialize the Whisper client
     */
    fun initialize() {
        try {
            Log.d(TAG, "Initializing Whisper client")
            
            // Initialize OkHttp client with longer timeouts for audio processing
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            // Get config manager for API keys
            configManager = ConfigManager.getInstance()
            
            isInitialized = true
            Log.i(TAG, "Whisper client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Whisper client: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Transcribe audio file using Whisper API
     */
    suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            Log.e(TAG, "Whisper client not initialized")
            throw IllegalStateException("Whisper client not initialized")
        }
        
        Log.d(TAG, "Transcribing audio file: ${audioFile.name}")
        
        try {
            // Get OpenAI API key from config
            val apiKey = configManager.getProperty("openai_api_key")
            if (apiKey.isBlank()) {
                Log.e(TAG, "OpenAI API key not found in config")
                throw IllegalStateException("OpenAI API key not found")
            }
            
            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", 
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
                )
                .addFormDataPart("model", "whisper-1")
                .addFormDataPart("language", "en")
                .build()
            
            // Create request
            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()
            
            Log.d(TAG, "Sending audio transcription request to OpenAI")
            
            // Execute request
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Error from Whisper API: ${response.code}, body: $errorBody")
                throw IOException("Whisper API error: ${response.code}")
            }
            
            // Parse response
            val responseBody = response.body?.string() ?: throw IOException("Empty response from OpenAI")
            val jsonResponse = JSONObject(responseBody)
            val transcript = jsonResponse.optString("text", "")
            
            Log.i(TAG, "Transcription successful: '$transcript'")
            return@withContext transcript
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio: ${e.message}", e)
            throw e
        }
    }
} 