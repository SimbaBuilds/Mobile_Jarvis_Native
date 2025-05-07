import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useVoice } from '../context/VoiceContext';

interface VoiceAssistantProps {
  onSpeechResult: (text: string) => void;
}

export const VoiceAssistant: React.FC<VoiceAssistantProps> = ({ onSpeechResult }) => {
  const { isListening, startListening, stopListening } = useVoice();

  const handlePress = async () => {
    if (isListening) {
      await stopListening();
    } else {
      await startListening();
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={[styles.button, isListening && styles.buttonActive]}
        onPress={handlePress}
      >
        <Text style={styles.buttonText}>
          {isListening ? 'Stop Listening' : 'Start Listening'}
        </Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    padding: 16,
  },
  button: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 24,
  },
  buttonActive: {
    backgroundColor: '#F44336',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '500',
  },
}); 