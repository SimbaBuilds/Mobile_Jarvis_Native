import { useState, useEffect, useCallback } from 'react';
import { Alert, Platform } from 'react-native';
import WakeWordService from '../../../services/NativeModules/WakeWordService';
import { useVoiceState } from './useVoiceState';

/**
 * Hook for wake word detection functionality
 */
export const useWakeWord = () => {
  const [isAvailable, setIsAvailable] = useState<boolean>(false);
  const [isActive, setIsActive] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  // Get voice state context to coordinate with voice recognition
  const { voiceState } = useVoiceState();
  
  // Check if wake word detection is available on this device
  useEffect(() => {
    const checkAvailability = async () => {
      setIsLoading(true);
      try {
        const available = await WakeWordService.isAvailable();
        setIsAvailable(available);
        
        if (!available && Platform.OS === 'android') {
          setError('Wake word detection is not available on this device');
        }
      } catch (err) {
        setError(`Error checking wake word availability: ${err}`);
        console.error('Error in useWakeWord:', err);
      } finally {
        setIsLoading(false);
      }
    };
    
    checkAvailability();
  }, []);
  
  // Start wake word detection
  const startDetection = useCallback(async () => {
    if (!isAvailable) {
      setError('Wake word detection is not available on this device');
      return false;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      const result = await WakeWordService.startDetection();
      
      if (result.success) {
        setIsActive(true);
        return true;
      } else {
        setError(result.error || 'Failed to start wake word detection');
        if (result.error?.includes('permission')) {
          Alert.alert(
            'Microphone Permission Required',
            'Wake word detection requires microphone permission.',
            [{ text: 'OK' }]
          );
        }
        return false;
      }
    } catch (err) {
      setError(`Error starting wake word detection: ${err}`);
      console.error('Error in startDetection:', err);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [isAvailable]);
  
  // Stop wake word detection
  const stopDetection = useCallback(async () => {
    if (!isActive) {
      return true;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      const result = await WakeWordService.stopDetection();
      
      if (result.success) {
        setIsActive(false);
        return true;
      } else {
        setError(result.error || 'Failed to stop wake word detection');
        return false;
      }
    } catch (err) {
      setError(`Error stopping wake word detection: ${err}`);
      console.error('Error in stopDetection:', err);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [isActive]);
  
  // Toggle wake word detection
  const toggleDetection = useCallback(async () => {
    return isActive ? await stopDetection() : await startDetection();
  }, [isActive, startDetection, stopDetection]);
  
  // Set the Picovoice API key
  const setAccessKey = useCallback(async (accessKey: string) => {
    try {
      const result = await WakeWordService.setAccessKey(accessKey);
      return result.success;
    } catch (err) {
      console.error('Error setting access key:', err);
      return false;
    }
  }, []);
  
  return {
    isAvailable,
    isActive,
    isLoading,
    error,
    startDetection,
    stopDetection,
    toggleDetection,
    setAccessKey,
  };
};
