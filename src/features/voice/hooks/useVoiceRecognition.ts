import { useState, useCallback, useEffect } from 'react';
import { useVoiceState } from '../context/VoiceContext';
import { VoiceState } from '../../../types/voice';

/**
 * Hook for handling voice recognition
 * 
 * Provides functions and state for controlling voice recognition and responding to voice state changes
 * 
 * @param onResult Optional callback function to handle recognized text
 * @param autoStart Whether to start listening automatically on mount
 * @returns Object containing voice recognition state and control functions
 */
export function useVoiceRecognition({
  onResult,
  autoStart = false,
}: {
  onResult?: (text: string) => void;
  autoStart?: boolean;
} = {}) {
  const { 
    voiceState, 
    isListening, 
    isSpeaking, 
    isError,
    startListening, 
    stopListening, 
    interruptSpeech
  } = useVoiceState();
  
  const [recognizedText, setRecognizedText] = useState<string>('');
  const [isReady, setIsReady] = useState<boolean>(false);
  
  // Start listening handler
  const handleStartListening = useCallback(async () => {
    if (isListening || isSpeaking) {
      console.log('Already listening or speaking, not starting');
      return false;
    }
    
    const result = await startListening();
    
    if (result) {
      setRecognizedText(''); // Clear previous results when starting new recognition
    }
    
    return result;
  }, [isListening, isSpeaking, startListening]);
  
  // Stop listening handler
  const handleStopListening = useCallback(async () => {
    if (!isListening) {
      console.log('Not listening, no need to stop');
      return true;
    }
    
    return await stopListening();
  }, [isListening, stopListening]);
  
  // Cancel current recognition/speech
  const cancelVoice = useCallback(async () => {
    if (isSpeaking) {
      return await interruptSpeech();
    } else if (isListening) {
      return await stopListening();
    }
    return true;
  }, [isSpeaking, isListening, interruptSpeech, stopListening]);
  
  // Process voice state changes
  useEffect(() => {
    // When voice state changes to PROCESSING, we know speech was recognized
    if (voiceState === VoiceState.PROCESSING) {
      // In a real implementation, we would get the recognized text here
      // For now, we can only mock this
      const mockText = "This is mock recognized text";
      setRecognizedText(mockText);
      
      // Call onResult callback if provided
      if (onResult) {
        onResult(mockText);
      }
    }
  }, [voiceState, onResult]);
  
  // Auto-start on mount if requested
  useEffect(() => {
    setIsReady(true);
    
    if (autoStart) {
      handleStartListening();
    }
    
    // Cleanup on unmount
    return () => {
      if (isListening || isSpeaking) {
        cancelVoice();
      }
    };
  }, [autoStart, handleStartListening, isListening, isSpeaking, cancelVoice]);
  
  return {
    isReady,
    isListening,
    isSpeaking,
    isError,
    voiceState,
    recognizedText,
    startListening: handleStartListening,
    stopListening: handleStopListening,
    cancelVoice,
  };
}
