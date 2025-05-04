import React, { useState, useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import { VoiceButton } from './VoiceButton';
import { VoiceResponseDisplay } from './VoiceResponseDisplay';
import { useVoiceRecognition } from '../../features/voice/hooks/useVoiceRecognition';
import { VoiceStatusIndicator } from '../../features/voice/components/VoiceStatusIndicator';

interface VoiceAssistantProps {
  onSpeechResult?: (text: string) => void;
}

/**
 * Main component for voice assistant functionality
 * Combines voice button, response display, and status indicator
 */
export const VoiceAssistant: React.FC<VoiceAssistantProps> = ({ 
  onSpeechResult 
}) => {
  const [response, setResponse] = useState<string>('');
  
  // Use the voice recognition hook
  const { 
    isListening,
    isSpeaking,
    recognizedText,
    voiceState
  } = useVoiceRecognition({
    onResult: (text) => {
      if (text) {
        if (onSpeechResult) {
          onSpeechResult(text);
        }
        // In a real app, we would send the text to an API and get a response
        // For now, we'll just echo it back
        const mockResponse = `I heard: "${text}"`;
        setResponse(mockResponse);
      }
    }
  });
  
  // Clear response when returning to idle
  useEffect(() => {
    if (!isListening && !isSpeaking) {
      // Delay clearing so user can see the last response
      const timer = setTimeout(() => {
        setResponse('');
      }, 5000);
      
      return () => clearTimeout(timer);
    }
  }, [isListening, isSpeaking]);
  
  return (
    <View style={styles.container}>
      <VoiceStatusIndicator />
      
      {response ? (
        <VoiceResponseDisplay
          text={response}
          style={styles.responseContainer}
        />
      ) : null}
      
      <View style={styles.buttonContainer}>
        <VoiceButton />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    padding: 16,
  },
  responseContainer: {
    flex: 1,
    justifyContent: 'center',
    padding: 16,
  },
  buttonContainer: {
    alignItems: 'center',
    padding: 20,
  },
});
