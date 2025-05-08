package com.anonymous.MobileJarvisNative.voice

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Bridge module for exposing Voice functionality to React Native
 */
class VoiceModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val TAG = "VoiceModule"
    private val voiceManager = VoiceManager.getInstance(reactContext)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun getName(): String {
        return "VoiceModule"
    }

    /**
     * Start listening for voice input
     */
    @ReactMethod
    fun startListening(promise: Promise) {
        Log.d(TAG, "startListening called from JS")
        try {
            voiceManager.startListening()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting listening", e)
            promise.reject("ERR_VOICE_START", e.message, e)
        }
    }

    /**
     * Stop listening for voice input
     */
    @ReactMethod
    fun stopListening(promise: Promise) {
        Log.d(TAG, "stopListening called from JS")
        try {
            voiceManager.stopListening()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listening", e)
            promise.reject("ERR_VOICE_STOP", e.message, e)
        }
    }

    /**
     * Interrupt any ongoing speech
     */
    @ReactMethod
    fun interruptSpeech(promise: Promise) {
        Log.d(TAG, "interruptSpeech called from JS")
        try {
            voiceManager.interruptSpeech()
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error interrupting speech", e)
            promise.reject("ERR_VOICE_INTERRUPT", e.message, e)
        }
    }

    /**
     * Get the current voice state
     */
    @ReactMethod
    fun getVoiceState(promise: Promise) {
        try {
            val state = voiceManager.voiceState.value.name
            promise.resolve(state)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting voice state", e)
            promise.reject("ERR_VOICE_STATE", e.message, e)
        }
    }

    /**
     * Add a listener for voice state changes
     */
    @ReactMethod
    fun addListener(eventName: String) {
        // Required for RN built in Event Emitter
        if (eventName == "onVoiceStateChange") {
            // Set up the state flow collector if not already set up
            setupStateFlowListener()
        }
    }

    /**
     * Remove listeners
     */
    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for RN built in Event Emitter
        // Real removal happens in VoiceService class
    }

    /**
     * Initialize state flow listener
     */
    private fun setupStateFlowListener() {
        voiceManager.voiceState
            .onEach { state ->
                // Send the state update to JS
                sendEvent("onVoiceStateChange", mapOf("state" to state.name))
            }
            .launchIn(coroutineScope)
    }

    /**
     * Send an event to JavaScript
     */
    private fun sendEvent(eventName: String, params: Map<String, Any>) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
} 