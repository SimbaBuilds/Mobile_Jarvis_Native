package com.anonymous.MobileJarvisNative.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Simple API client for making direct HTTP requests to the backend server
 */
class ApiClient {
    private val TAG = "ApiClient"
    
    // Initialize client with reasonable timeouts
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Send a text request to the backend server (synchronous)
     */
    fun sendTextRequestSync(text: String): String {
        try {
            // Try multiple server URLs in order of preference
            val urls = listOf(
                "http://0.0.0.0:8000/api/chat",
                "http://localhost:8000/api/chat", 
                "http://192.168.1.131:8000/api/chat",
                "https://mobile-jarvis-backend.onrender.com/api/chat"
            )
            
            // Create JSON payload
            val json = JSONObject().apply {
                put("message", text)
                put("timestamp", System.currentTimeMillis())
                put("history", JSONArray())
            }
            
            val jsonString = json.toString()
            Log.d(TAG, "Request payload: $jsonString")
            val requestBody = jsonString.toRequestBody("application/json".toMediaType())
            
            // Try each URL in sequence
            for (url in urls) {
                try {
                    Log.i(TAG, "Sending request to server: $url")
                    
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()
                    
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "{}"
                        Log.i(TAG, "Server response: $responseBody")
                        
                        val responseJson = JSONObject(responseBody)
                        return responseJson.optString("response", "No response from server")
                    } else {
                        Log.w(TAG, "Request to $url failed with status ${response.code}. Response: ${response.body?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending request to $url: ${e.message}", e)
                }
            }
            
            // If we've tried all URLs and none worked
            return "Error: Could not connect to any available server"
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendTextRequestSync", e)
            return "Error processing request: ${e.message}"
        }
    }
} 