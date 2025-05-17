import React, { createContext, useContext, useState, useCallback, useEffect, ReactNode, useMemo, useRef } from 'react';
import { VoiceState, VoiceContextValue } from './types/voice';
import VoiceService from './VoiceService';
import { useServerApi } from '../../api/useServerApi';
import { useVoiceState as useVoiceStateHook } from './hooks/useVoiceState';

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
  type: 'text' | 'image';
  timestamp: number;
}

/**
 * Provider component for the Voice Context
 */
export const VoiceProvider: React.FC<VoiceProviderProps> = ({ children }) => {
  // Use the voice state hook to access native state
  const voiceStateFromHook = useVoiceStateHook();
  
  // Use the voice state from the hook rather than managing our own
  const voiceState = voiceStateFromHook.voiceState;
  const isListening = voiceStateFromHook.isListening;
  const isSpeaking = voiceStateFromHook.isSpeaking;
  const isError = voiceStateFromHook.isError;
  
  // Other state we still need to manage
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
          type: 'text',
          timestamp: apiResponse.timestamp || Date.now()
        };
        
        console.log('Adding assistant message to chat history:', assistantMessage);
        setChatHistory(prevHistory => [...prevHistory, assistantMessage]);
        
        // Use TTS to speak the response
        speakResponse(apiResponse.response);
      }
    },
    onError: (apiError) => {
      console.error('API error:', apiError);
      setError(`Error communicating with server: ${apiError.message}`);
    }
  });
  
  // Log component mount and initial state
  useEffect(() => {
    console.log('🎤 VoiceProvider mounted');
    console.log('🎤 Initial voice state:', voiceState);
    console.log('🎤 Initial wake word enabled:', isWakeWordEnabled);
    return () => {
      console.log('🎤 VoiceProvider unmounting');
    };
  }, []);

  // Log when wake word state changes for debugging purposes
  useEffect(() => {
    console.log(`🎤 Wake word enabled state changed to: ${isWakeWordEnabled}`);
  }, [isWakeWordEnabled]);

  // Log when voice state changes
  useEffect(() => {
    console.log(`🎙️ Voice state changed to: ${voiceState}`);
  }, [voiceState]);

  // Log when listening/speaking states change
  useEffect(() => {
    console.log(`🔊 Listening: ${isListening}, Speaking: ${isSpeaking}`);
  }, [isListening, isSpeaking]);
  
  // Set up voice service event listeners
  useEffect(() => {
    console.log('🎤 Setting up speech result listener');
    // Speech result listener
    const speechResultUnsubscribe = voiceService.onSpeechResult((event) => {
      console.log('🎤 Speech result received:', event.text);
      console.log('🎤 Current voice state:', voiceState);
      setTranscript(event.text);
      
      // Add user message to chat history
      const userMessage: ChatMessage = {
        role: 'user',
        content: event.text,
        type: 'text',
        timestamp: Date.now()
      };
      console.log('💬 Adding user message to chat history:', userMessage);
      setChatHistory(prevHistory => {
        const newHistory = [...prevHistory, userMessage];
        console.log('📜 Updated chat history length:', newHistory.length);
        return newHistory;
      });
      // Process the message with the server API
      console.log('🌐 Processing speech with server API');
      processSpeechWithServer(event.text, [...chatHistory, userMessage]);
    });
    
    return () => {
      // Cleanup listeners on unmount
      console.log('Cleaning up speech result listener');
      speechResultUnsubscribe();
    };
  }, [voiceService, chatHistory]);
  
  // Process speech with server API
  const processSpeechWithServer = useCallback(async (speechText: string, currentHistory: ChatMessage[]) => {
    try {
      console.log('🌐 Processing speech with server API:', speechText);
      console.log('📜 Current history length:', currentHistory.length);
      const response = await serverApi.sendMessage(speechText, currentHistory);
      return response;
    } catch (err) {
      // Error handling is done in onError callback
      console.error('Error in processSpeechWithServer:', err);
      return null;
    }
  }, [serverApi]);
  
  // Use directly the startListening from hook
  const startListening = voiceStateFromHook.startListening;
  
  // Use native TTS to speak the response
  const speakResponse = useCallback(async (responseText: string) => {
    try {
      console.log('Speaking response:', responseText);
      
      // Already handled by the native side
      await voiceService.speakResponse(responseText);
      
      // The native side will automatically start listening again
      console.log('Response spoken, native side will automatically restart listening');
    } catch (err) {
      console.error('Error speaking response:', err);
      setError(`Error speaking response: ${err}`);
    }
  }, [voiceService]);
  
  // Use directly the stopListening from hook
  const stopListening = voiceStateFromHook.stopListening;
  
  // Reset state to default (just transcript and response as we're using the hook for state)
  const resetState = useCallback(() => {
    setError(null);
    setTranscript('');
    setResponse('');
    // We don't reset isWakeWordEnabled or chatHistory here as those are persistent
  }, []);
  
  // Clear chat history
  const clearChatHistory = useCallback(() => {
    setChatHistory([]);
  }, []);
  
  // Use directly the interruptSpeech from hook
  const interruptSpeech = useCallback(async () => {
    try {
      console.log('🛑 Interrupting current speech');
      
      // Call the hook's interruptSpeech function
      const result = await voiceStateFromHook.interruptSpeech();
      
      if (result) {
        // Set response to empty to clear any visible response text
        setResponse('');
        
        console.log('✅ Speech interrupted successfully, transitioning to LISTENING state');
        
        // Add a short delay to allow for native side state updates
        setTimeout(() => {
          // The native side should handle the transition to LISTENING state
          // But we can log for debugging purposes
          console.log('🎤 State after interruption:', voiceStateFromHook.voiceState);
        }, 500);
      } else {
        console.log('❌ Failed to interrupt speech');
      }
      
      return result;
    } catch (err) {
      console.error('Error interrupting speech:', err);
      return false;
    }
  }, [voiceStateFromHook]);
  
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
    setVoiceState: () => {}, // This is now handled by the native side
    setWakeWordEnabled,
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
