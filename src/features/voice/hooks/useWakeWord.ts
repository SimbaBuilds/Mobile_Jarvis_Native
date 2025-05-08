import { useState, useEffect, useCallback, useRef } from 'react';
import { Alert, Platform, NativeModules } from 'react-native';
import WakeWordService, { WakeWordEvents } from '../../../shared/services/NativeModules/WakeWordService';
import { useVoiceState } from './useVoiceState';

/**
 * Hook for wake word detection functionality
 */
export const useWakeWord = () => {
  const [isAvailable, setIsAvailable] = useState<boolean>(false);
  const [isActive, setIsActive] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  // Track if initialization is complete
  const initialized = useRef(false);
  
  // Get voice state context to coordinate with voice recognition
  const { voiceState, setWakeWordEnabled } = useVoiceState();
  
  // Check if wake word detection is available on this device and load saved state
  useEffect(() => {
    const checkAvailabilityAndStatus = async () => {
      setIsLoading(true);
      try {
        // Debug: Print all available native modules
        console.log('Available native modules:', Object.keys(NativeModules));
        console.log('Platform:', Platform.OS);
        
        // First check if the feature is available
        const available = await WakeWordService.isAvailable();
        console.log('WakeWord availability check returned:', available);
        setIsAvailable(available);
        
        if (!available) {
          if (Platform.OS === 'android') {
            setError('Wake word detection is not available on this device');
          }
          setIsLoading(false);
          return;
        }
        
        // If available, check if it was previously enabled
        const wasEnabled = await WakeWordService.getStatus();
        console.log('WakeWord status check returned:', wasEnabled);
        setIsActive(wasEnabled);
        
        // Update the voice context 
        setWakeWordEnabled(wasEnabled);
        
        // Mark as initialized
        initialized.current = true;
      } catch (err) {
        setError(`Error checking wake word availability: ${err}`);
        console.error('Error in useWakeWord:', err);
      } finally {
        setIsLoading(false);
      }
    };
    
    checkAvailabilityAndStatus();
    
    // Set up event listener for service restoration
    const subscription = WakeWordService.addListener(
      WakeWordEvents.SERVICE_RESTORED,
      (event) => {
        if (event.restored) {
          console.log('Wake word service was restored from previous session');
          setIsActive(true);
          setWakeWordEnabled(true);
        }
      }
    );
    
    return () => {
      subscription?.remove();
    };
  }, [setWakeWordEnabled]);
  
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
      return await WakeWordService.setAccessKey(accessKey);
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
