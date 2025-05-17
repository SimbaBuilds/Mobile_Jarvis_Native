import React, { useCallback } from 'react';
import { TouchableOpacity, StyleSheet, View, ActivityIndicator } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useVoiceState } from '../VoiceContext';
import { VoiceState } from '../VoiceService';

interface VoiceButtonProps {
  size?: number;
  color?: string;
  activeColor?: string;
  errorColor?: string;
  onPress?: () => void;
}

/**
 * Button component for activating voice recognition
 */
export const VoiceButton: React.FC<VoiceButtonProps> = ({
  size = 60,
  color = '#3498db',
  activeColor = '#2ecc71',
  errorColor = '#e74c3c',
  onPress,
}) => {
  const { voiceState, isListening, isSpeaking, isError, startListening, stopListening, interruptSpeech } = useVoiceState();
  
  // Handle button press based on current state
  const handlePress = useCallback(async () => {
    if (onPress) {
      onPress();
    }
    
    if (isListening) {
      // If currently listening, stop listening
      await stopListening();
    } else if (isSpeaking) {
      // If currently speaking, interrupt the speech
      await interruptSpeech();
    } else {
      // Otherwise, start listening
      await startListening();
    }
  }, [isListening, isSpeaking, startListening, stopListening, interruptSpeech, onPress]);
  
  // Determine the icon based on state
  const getIcon = () => {
    if (isListening) {
      return 'mic';
    } else if (isSpeaking) {
      return 'volume-high';
    } else if (isError) {
      return 'warning';
    }
    return 'mic-outline';
  };
  
  // Determine the color based on state
  const getColor = () => {
    if (isError) return errorColor;
    if (isListening || isSpeaking) return activeColor;
    return color;
  };
  
  return (
    <TouchableOpacity
      onPress={handlePress}
      style={[
        styles.button,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: getColor(),
        },
      ]}
      activeOpacity={0.8}
    >
      {(voiceState === VoiceState.PROCESSING) ? (
        <ActivityIndicator color="white" size="small" />
      ) : (
        <Ionicons name={getIcon()} size={size / 2} color="white" />
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 5, // Android shadow
    shadowColor: '#000', // iOS shadow
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
  },
});
