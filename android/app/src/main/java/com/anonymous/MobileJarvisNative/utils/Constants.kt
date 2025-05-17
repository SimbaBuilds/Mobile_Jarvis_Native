package com.anonymous.MobileJarvisNative.utils

/**
 * Application-wide constants
 */
object Constants {
    // Wake word constants
    const val WAKE_WORD_ACTION = "com.anonymous.MobileJarvisNative.WAKE_WORD_DETECTED"
    
    // Notification constants
    const val NOTIFICATION_CHANNEL_ID = "voice_assistant_channel"
    const val NOTIFICATION_ID = 1001
    
    // Shared preferences keys
    object Prefs {
        const val WAKE_WORD_ENABLED = "wake_word_enabled"
        const val ASSISTANT_VOLUME = "assistant_volume"
        const val USE_CUSTOM_LLM = "use_custom_llm"
        const val CUSTOM_LLM_URL = "custom_llm_url"
        const val CUSTOM_LLM_KEY = "custom_llm_key"
    }
    
    // Config property keys
    object Config {
        const val PICOVOICE_ACCESS_KEY = "picovoice_access_key"
        const val DEEPGRAM_API_KEY = "deepgram_api_key"
        const val ELEVENLABS_API_KEY = "elevenlabs_api_key"
        const val OPENAI_API_KEY = "openai_api_key"
        const val SERVER_API_BASE_URL = "server_api_base_url"
        const val SERVER_API_ENDPOINT = "server_api_endpoint"
        
        // Speech recognition config keys
        const val SPEECH_RECOGNITION_MINIMUM_LENGTH_MS = "speech_recognition_minimum_length_ms"
        const val SPEECH_RECOGNITION_COMPLETE_SILENCE_MS = "speech_recognition_complete_silence_ms"
        const val SPEECH_RECOGNITION_POSSIBLE_SILENCE_MS = "speech_recognition_possible_silence_ms"
        const val SPEECH_RETRY_DELAY_MS = "speech_retry_delay_ms"
        const val SPEECH_FINAL_MESSAGE_DELAY_MS = "speech_final_message_delay_ms"
        const val SPEECH_MAX_NO_SPEECH_RETRIES = "speech_max_no_speech_retries"
        const val USE_CUSTOM_RECOGNIZER_PARAMS = "use_custom_recognizer_params"
    }
    
    // Intent extras
    object Extras {
        const val COMMAND_TEXT = "command_text"
        const val RESPONSE_TEXT = "response_text"
    }
    
    // Request codes
    object RequestCodes {
        const val SPEECH_RECOGNITION = 1001
        const val PERMISSIONS = 1002
        const val BATTERY_OPTIMIZATION = 1003
    }
    
    // Permissions
    object Permissions {
        val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.INTERNET
        )
        
        val OPTIONAL_PERMISSIONS = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_CONTACTS
        )
    }
    
    object Actions {
        // Wake word actions
        const val WAKE_WORD_DETECTED_RN = "com.anonymous.MobileJarvisNative.WAKE_WORD_DETECTED_RN"
        const val PAUSE_WAKE_WORD_KEEP_LISTENING = "com.anonymous.MobileJarvisNative.PAUSE_WAKE_WORD_KEEP_LISTENING"
        const val RESUME_WAKE_WORD = "com.anonymous.MobileJarvisNative.RESUME_WAKE_WORD"
        
        // Voice events
        const val SPEECH_RESULT = "speechResult"
        const val ASSISTANT_RESPONSE = "assistantResponse"
        const val VOICE_STATE_CHANGE = "onVoiceStateChange"
    }
} 