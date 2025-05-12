import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode, useMemo } from 'react';
import { VoiceState, VoiceContextValue } from './types/voice';
import VoiceService from './VoiceService';
import { useServerApi } from '../../api/useServerApi';

// Create context with default values
const VoiceContext = createContext<VoiceContextValue>({
  voiceState: VoiceState.IDLE,
  isWakeWordEnabled: false,
  error: null,
  transcript: '',
  response: '',
  chatHistory: [],
  isListening: false,
  isSpeaking: false,
  isError: false,
  setVoiceState: () => {},
  setWakeWordEnabled: () => {},
  setError: () => {},
  setTranscript: () => {},
  setResponse: () => {},
  startListening: async () => false,
  stopListening: async () => false,
  resetState: () => {},
  interruptSpeech: async () => false,
  clearChatHistory: () => {},
});

interface VoiceProviderProps {
  children: ReactNode;
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
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
  const [chatHistory, setChatHistory] = useState<ChatMessage[]>([]);
  
  // Voice service instance
  const voiceService = useMemo(() => VoiceService.getInstance(), []);
  
  // Server API hook
  const serverApi = useServerApi({
    preferences: {
      voice: 'male',
      response_type: 'concise'
    },
    onResponse: (apiResponse) => {
      console.log('Received API response:', apiResponse);
      
      if (apiResponse.response) {
        setResponse(apiResponse.response);
        
        // Add assistant message to chat history
        const assistantMessage: ChatMessage = {
          role: 'assistant',
          content: apiResponse.response,
          timestamp: apiResponse.timestamp || Date.now()
        };
        
        setChatHistory(prevHistory => [...prevHistory, assistantMessage]);
        
        // Use TTS to speak the response
        speakResponse(apiResponse.response);
      }
    },
    onError: (apiError) => {
      console.error('API error:', apiError);
      setError(`Error communicating with server: ${apiError.message}`);
      setVoiceState(VoiceState.ERROR);
    }
  });
  
  // Computed state
  const isListening = voiceState === VoiceState.LISTENING;
  const isSpeaking = voiceState === VoiceState.SPEAKING;
  const isError = voiceState === VoiceState.ERROR;
  
  // Log when wake word state changes for debugging purposes
  useEffect(() => {
    console.log(`Wake word enabled state changed to: ${isWakeWordEnabled}`);
  }, [isWakeWordEnabled]);
  
  // Set up voice service event listeners
  useEffect(() => {
    // Speech result listener
    const speechResultUnsubscribe = voiceService.onSpeechResult((event) => {
      console.log('Speech result received:', event.text);
      setTranscript(event.text);
      
      // Add user message to chat history
      const userMessage: ChatMessage = {
        role: 'user',
        content: event.text,
        timestamp: Date.now()
      };
      
      setChatHistory(prevHistory => [...prevHistory, userMessage]);
      
      // Process the message with the server API
      setVoiceState(VoiceState.PROCESSING);
      processSpeechWithServer(event.text, [...chatHistory, userMessage]);
    });
    
    // Voice state change listener
    const voiceStateUnsubscribe = voiceService.onVoiceStateChange((event) => {
      console.log('Voice state changed:', event.state);
      setVoiceState(event.state as VoiceState);
    });
    
    return () => {
      // Cleanup listeners on unmount
      speechResultUnsubscribe();
      voiceStateUnsubscribe();
    };
  }, [voiceService, chatHistory]);
  
  // Process speech with server API
  const processSpeechWithServer = useCallback(async (speechText: string, currentHistory: ChatMessage[]) => {
    try {
      console.log('Processing speech with server API:', speechText);
      setVoiceState(VoiceState.PROCESSING);
      
      const response = await serverApi.sendMessage(speechText, currentHistory);
      
      // State is already updated via onResponse callback
      setVoiceState(VoiceState.SPEAKING);
      
      return response;
    } catch (err) {
      // Error handling is done in onError callback
      console.error('Error in processSpeechWithServer:', err);
      return null;
    }
  }, [serverApi]);
  
  // Use native TTS to speak the response
  const speakResponse = useCallback(async (responseText: string) => {
    try {
      console.log('Speaking response:', responseText);
      setVoiceState(VoiceState.SPEAKING);
      
      // Use native module for TTS
      await voiceService.speakResponse(responseText);
      
      // After speaking is done, reset to idle state
      setVoiceState(VoiceState.IDLE);
    } catch (err) {
      console.error('Error speaking response:', err);
      setError(`Error speaking response: ${err}`);
      setVoiceState(VoiceState.ERROR);
    }
  }, [voiceService]);
  
  // Start listening for voice input
  const startListening = useCallback(async (): Promise<boolean> => {
    try {
      console.log('Starting voice listening...');
      setVoiceState(VoiceState.LISTENING);
      setError(null);
      
      const result = await voiceService.startListening();
      return result;
    } catch (err) {
      console.error('Error starting voice listening:', err);
      setError(`Error starting voice listening: ${err}`);
      setVoiceState(VoiceState.ERROR);
      return false;
    }
  }, [voiceService]);
  
  // Stop listening for voice input
  const stopListening = useCallback(async (): Promise<boolean> => {
    try {
      if (voiceState === VoiceState.LISTENING) {
        console.log('Stopping voice listening...');
        setVoiceState(VoiceState.IDLE);
        
        const result = await voiceService.stopListening();
        return result;
      }
      return true;
    } catch (err) {
      console.error('Error stopping voice listening:', err);
      setError(`Error stopping voice listening: ${err}`);
      setVoiceState(VoiceState.ERROR);
      return false;
    }
  }, [voiceState, voiceService]);
  
  // Reset state to default
  const resetState = useCallback(() => {
    setVoiceState(VoiceState.IDLE);
    setError(null);
    setTranscript('');
    setResponse('');
    // We don't reset isWakeWordEnabled or chatHistory here as those are persistent
  }, []);
  
  // Clear chat history
  const clearChatHistory = useCallback(() => {
    setChatHistory([]);
  }, []);
  
  // Effect to handle wake word detection state change
  useEffect(() => {
    if (voiceState === VoiceState.WAKE_WORD_DETECTED) {
      // Wake word was detected, start listening
      startListening();
    }
  }, [voiceState, startListening]);
  
  // Safe wrapper for setWakeWordEnabled to ensure proper typing
  const handleSetWakeWordEnabled = useCallback((enabled: boolean) => {
    setWakeWordEnabled(enabled);
  }, []);
  
  // Interrupt current speech
  const interruptSpeech = useCallback(async (): Promise<boolean> => {
    try {
      if (isSpeaking) {
        console.log('Interrupting speech...');
        const result = await voiceService.interruptSpeech();
        setVoiceState(VoiceState.IDLE);
        return result;
      }
      return false;
    } catch (err) {
      console.error('Error interrupting speech:', err);
      setError(`Error interrupting speech: ${err}`);
      setVoiceState(VoiceState.ERROR);
      return false;
    }
  }, [isSpeaking, voiceService]);
  
  // Context value
  const value: VoiceContextValue = {
    voiceState,
    isWakeWordEnabled,
    error,
    transcript,
    response,
    chatHistory,
    isListening,
    isSpeaking,
    isError,
    setVoiceState,
    setWakeWordEnabled: handleSetWakeWordEnabled,
    setError,
    setTranscript,
    setResponse,
    startListening,
    stopListening,
    resetState,
    interruptSpeech,
    clearChatHistory,
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

export const useVoice = () => {
  const context = useContext(VoiceContext);
  if (!context) {
    throw new Error('useVoice must be used within a VoiceProvider');
  }
  return context;
};
