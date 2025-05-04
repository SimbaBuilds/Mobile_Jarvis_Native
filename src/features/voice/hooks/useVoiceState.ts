import { useState, useEffect } from 'react';
import VoiceService, { VoiceState, VoiceStateChangeEvent } from '../../../services/NativeModules/VoiceService';

/**
 * Hook for accessing and managing voice state
 * 
 * @returns Object containing voice state and control functions
 */
export function useVoiceState() {
  const [voiceState, setVoiceState] = useState<VoiceState>(VoiceState.IDLE);
  const [isListening, setIsListening] = useState<boolean>(false);
  const [isSpeaking, setIsSpeaking] = useState<boolean>(false);
  const [error, setError] = useState<boolean>(false);

  // Update derived states when voice state changes
  useEffect(() => {
    setIsListening(
      voiceState === VoiceState.LISTENING || 
      voiceState === VoiceState.WAKE_WORD_DETECTED
    );
    
    setIsSpeaking(voiceState === VoiceState.SPEAKING);
    setError(voiceState === VoiceState.ERROR);
  }, [voiceState]);

  // Set up listener for voice state changes from native module
  useEffect(() => {
    // Get initial state
    VoiceService.getVoiceState()
      .then((state) => {
        setVoiceState(state as VoiceState);
      })
      .catch((err) => {
        console.error('Error getting voice state:', err);
      });

    // Add listener for state changes
    const unsubscribe = VoiceService.onVoiceStateChange((event: VoiceStateChangeEvent) => {
      setVoiceState(event.state);
    });

    // Clean up
    return () => {
      unsubscribe();
    };
  }, []);

  // Functions to control voice state
  const startListening = async () => {
    try {
      await VoiceService.startListening();
      return true;
    } catch (error) {
      console.error('Error starting voice recognition:', error);
      return false;
    }
  };

  const stopListening = async () => {
    try {
      await VoiceService.stopListening();
      return true;
    } catch (error) {
      console.error('Error stopping voice recognition:', error);
      return false;
    }
  };

  const interruptSpeech = async () => {
    try {
      return await VoiceService.interruptSpeech();
    } catch (error) {
      console.error('Error interrupting speech:', error);
      return false;
    }
  };

  // Return the state and control functions
  return {
    voiceState,
    isListening,
    isSpeaking,
    isError: error,
    startListening,
    stopListening,
    interruptSpeech
  };
}
