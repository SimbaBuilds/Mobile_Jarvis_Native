package com.cameronhightower.aivoiceassistant.utils

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Utility functions for speech recognition
 */
object SpeechUtils {
    private const val TAG = "SpeechUtils"
    
    /**
     * Creates and configures a speech recognition intent
     * 
     * @param languageModel The language model to use
     * @param language The language code to use
     * @return Intent configured for speech recognition
     */
    fun createSpeechRecognitionIntent(
        languageModel: String = RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        language: String = "en-US"
    ): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, languageModel)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        }
    }
    
    /**
     * Checks if speech recognition is available on the device
     * 
     * @param context Application context
     * @return Boolean indicating if speech recognition is available
     */
    fun isSpeechRecognitionAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Gets a user-friendly error message for SpeechRecognizer error codes
     * 
     * @param errorCode The error code from SpeechRecognizer
     * @return A user-friendly error message
     */
    fun getSpeechRecognitionErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Unknown error: $errorCode"
        }
    }
} 