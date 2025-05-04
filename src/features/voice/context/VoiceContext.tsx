import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { VoiceState, VoiceContextValue } from '../../../types/voice';

// Create context with default values
const VoiceContext = createContext<VoiceContextValue>({
  voiceState: VoiceState.IDLE,
  isWakeWordEnabled: false,
  error: null,
  transcript: '',
  response: '',
  isListening: false,
  setVoiceState: () => {},
  setWakeWordEnabled: () => {},
  setError: () => {},
  setTranscript: () => {},
  setResponse: () => {},
  startListening: async () => false,
  stopListening: async () => false,
  resetState: () => {},
});

interface VoiceProviderProps {
  children: ReactNode;
}

/**
 * Provider component for the Voice Context
 */
export const VoiceProvider: React.FC<VoiceProviderProps> = ({ children }) => {
  // State
  const [voiceState, setVoiceState] = useState<VoiceState>(VoiceState.IDLE);
  const [isWakeWordEnabled, setWakeWordEnabled] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [transcript, setTranscript] = useState<string>('');
  const [response, setResponse] = useState<string>('');
  
  // Computed state
  const isListening = voiceState === VoiceState.LISTENING;
  
  // Start listening for voice input
  const startListening = useCallback(async (): Promise<boolean> => {
    try {
      setVoiceState(VoiceState.LISTENING);
      setError(null);
      
      // Here we would actually start voice recognition
      // For now, just simulating the state change
      
      return true;
    } catch (err) {
      console.error('Error starting voice listening:', err);
      setError(`Error starting voice listening: ${err}`);
      setVoiceState(VoiceState.ERROR);
      return false;
    }
  }, []);
  
  // Stop listening for voice input
  const stopListening = useCallback(async (): Promise<boolean> => {
    try {
      if (voiceState === VoiceState.LISTENING) {
        setVoiceState(VoiceState.IDLE);
      }
      
      // Here we would actually stop voice recognition
      // For now, just simulating the state change
      
      return true;
    } catch (err) {
      console.error('Error stopping voice listening:', err);
      setError(`Error stopping voice listening: ${err}`);
      setVoiceState(VoiceState.ERROR);
      return false;
    }
  }, [voiceState]);
  
  // Reset state to default
  const resetState = useCallback(() => {
    setVoiceState(VoiceState.IDLE);
    setError(null);
    setTranscript('');
    setResponse('');
  }, []);
  
  // Effect to handle wake word detection state change
  useEffect(() => {
    if (voiceState === VoiceState.WAKE_WORD_DETECTED) {
      // Wake word was detected, start listening
      startListening();
    }
  }, [voiceState, startListening]);
  
  // Context value
  const value: VoiceContextValue = {
    voiceState,
    isWakeWordEnabled,
    error,
    transcript,
    response,
    isListening,
    setVoiceState,
    setWakeWordEnabled,
    setError,
    setTranscript,
    setResponse,
    startListening,
    stopListening,
    resetState,
  };
  
  return <VoiceContext.Provider value={value}>{children}</VoiceContext.Provider>;
};

/**
 * Hook to access the voice context
 */
export const useVoiceState = (): VoiceContextValue => {
  const context = useContext(VoiceContext);
  
  if (!context) {
    throw new Error('useVoiceState must be used within a VoiceProvider');
  }
  
  return context;
};
