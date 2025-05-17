import { useState, useEffect } from 'react';
import VoiceService, { VoiceState, VoiceStateChangeEvent } from '../VoiceService';

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
  const [wakeWordEnabled, setWakeWordEnabled] = useState<boolean>(false);

  // Update derived states when voice state changes
  useEffect(() => {
    setIsListening(
      voiceState === VoiceState.LISTENING || 
      voiceState === VoiceState.WAKE_WORD_DETECTED
    );
    
    /**
     * Important: We treat both SPEAKING and RESPONDING states as speaking states
     * 
     * The native VoiceManager.kt has two separate states:
     * - SPEAKING: When TTS is playing
     * - RESPONDING(message): Contains the response text being spoken
     * 
     * By treating both as "isSpeaking", we ensure the interrupt button appears
     * in both states, letting users stop speech at any time.
     */
    setIsSpeaking(
      voiceState === VoiceState.SPEAKING || 
      String(voiceState).includes('RESPONDING')
    );
    
    setError(voiceState === VoiceState.ERROR);
  }, [voiceState]);

  // Set up listener for voice state changes from native module
  useEffect(() => {
    // Get initial state
    VoiceService.getInstance().getVoiceState()
      .then((state) => {
        setVoiceState(state as VoiceState);
      })
      .catch((err: Error) => {
        console.error('Error getting voice state:', err);
      });

    // Add listener for state changes
    const unsubscribe = VoiceService.getInstance().onVoiceStateChange((event: VoiceStateChangeEvent) => {
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
      await VoiceService.getInstance().startListening();
      return true;
    } catch (error) {
      console.error('Error starting voice recognition:', error);
      return false;
    }
  };

  const stopListening = async () => {
    try {
      await VoiceService.getInstance().stopListening();
      return true;
    } catch (error) {
      console.error('Error stopping voice recognition:', error);
      return false;
    }
  };

  /**
   * Interrupts current speech and transitions to LISTENING state
   * Calls the native interruptSpeech method that handles:
   * 1. Stopping TTS playback
   * 2. Changing state from RESPONDING/SPEAKING to LISTENING
   * 3. Restarting speech recognition
   */
  const interruptSpeech = async () => {
    try {
      return await VoiceService.getInstance().interruptSpeech();
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
    wakeWordEnabled,
    setWakeWordEnabled,
    startListening,
    stopListening,
    interruptSpeech
  };
}
