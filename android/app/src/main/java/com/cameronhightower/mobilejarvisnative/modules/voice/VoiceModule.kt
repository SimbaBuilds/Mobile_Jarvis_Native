package com.cameronhightower.mobilejarvisnative.modules.voice

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class VoiceModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val voiceProcessor = VoiceProcessor()
    private var isListening = false

    init {
        voiceProcessor.initialize(reactContext)
    }

    override fun getName() = "VoiceModule"

    @ReactMethod
    fun startListening(promise: Promise) {
        try {
            if (isListening) {
                promise.resolve(false)
                return
            }
            
            isListening = true
            voiceProcessor.startListening()
            sendEvent("onVoiceStateChange", Arguments.createMap().apply {
                putString("state", "LISTENING")
            })
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun stopListening(promise: Promise) {
        try {
            if (!isListening) {
                promise.resolve(true)
                return
            }
            
            isListening = false
            voiceProcessor.stopListening()
            sendEvent("onVoiceStateChange", Arguments.createMap().apply {
                putString("state", "IDLE")
            })
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun interruptSpeech(promise: Promise) {
        try {
            voiceProcessor.interruptSpeech()
            sendEvent("onVoiceStateChange", Arguments.createMap().apply {
                putString("state", "IDLE")
            })
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    @ReactMethod
    fun getVoiceState(promise: Promise) {
        try {
            val state = when {
                !isListening -> "IDLE"
                voiceProcessor.isProcessing() -> "PROCESSING"
                voiceProcessor.isSpeaking() -> "SPEAKING"
                else -> "LISTENING"
            }
            promise.resolve(state)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        voiceProcessor.release()
    }
} 