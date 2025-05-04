import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ViewStyle, Animated } from 'react-native';

interface VoiceResponseDisplayProps {
  text: string;
  style?: ViewStyle;
}

/**
 * Component to display responses from voice processing
 */
export const VoiceResponseDisplay: React.FC<VoiceResponseDisplayProps> = ({ 
  text, 
  style 
}) => {
  const [fadeAnim] = useState(new Animated.Value(0));
  
  // Animate in when text changes
  useEffect(() => {
    if (text) {
      // Reset opacity to 0 if needed
      fadeAnim.setValue(0);
      
      // Animate to fully visible
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 300,
        useNativeDriver: true,
      }).start();
    }
  }, [text, fadeAnim]);
  
  if (!text) return null;
  
  return (
    <Animated.View 
      style={[
        styles.container, 
        style,
        { opacity: fadeAnim }
      ]}
    >
      <View style={styles.bubble}>
        <Text style={styles.text}>{text}</Text>
      </View>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 20,
  },
  bubble: {
    backgroundColor: '#f1f1f1',
    borderRadius: 20,
    padding: 16,
    maxWidth: '80%',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
  },
  text: {
    fontSize: 16,
    color: '#333',
    lineHeight: 22,
  },
});
