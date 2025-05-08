import React, { useState, useEffect } from 'react';
import { View, Switch, Text, StyleSheet, Animated, Easing } from 'react-native';
import { useWakeWord } from '../WakeWordContext';
import WakeWordService, { WakeWordEvents } from '../WakeWordService';

export const WakeWordToggle: React.FC = () => {
    const { isEnabled, setEnabled } = useWakeWord();
    const [detectionAnimation] = useState(new Animated.Value(0));
    const [isDetected, setIsDetected] = useState(false);

    // Listen for wake word detection events
    useEffect(() => {
        const subscription = WakeWordService.addListener(
            'wakeWordDetected',
            () => {
                // Start the animation
                setIsDetected(true);
                
                // Animate the detection indicator
                Animated.sequence([
                    Animated.timing(detectionAnimation, {
                        toValue: 1,
                        duration: 300,
                        useNativeDriver: true,
                        easing: Easing.bounce,
                    }),
                    Animated.delay(1000),
                    Animated.timing(detectionAnimation, {
                        toValue: 0,
                        duration: 300,
                        useNativeDriver: true,
                    })
                ]).start(() => setIsDetected(false));
            }
        );
        
        return () => subscription?.remove();
    }, [detectionAnimation]);

    const handleToggle = async (value: boolean) => {
        try {
            await setEnabled(value);
        } catch (error) {
            console.error('Error toggling wake word:', error);
        }
    };

    // Animation styles
    const detectionIndicatorStyle = {
        opacity: detectionAnimation,
        transform: [
            {
                scale: detectionAnimation.interpolate({
                    inputRange: [0, 1],
                    outputRange: [1, 1.2],
                }),
            },
        ],
    };

    return (
        <View style={styles.container}>
            <View style={styles.row}>
                <Text style={styles.label}>Wake Word Detection</Text>
                <Switch
                    value={isEnabled}
                    onValueChange={handleToggle}
                    trackColor={{ false: '#767577', true: '#81b0ff' }}
                    thumbColor={isEnabled ? '#2196F3' : '#f4f3f4'}
                />
            </View>
            
            {isEnabled && (
                <View style={styles.statusContainer}>
                    <Text style={styles.statusText}>
                        Listening for "Jarvis"
                    </Text>
                    {isDetected && (
                        <Animated.View style={[styles.detectionIndicator, detectionIndicatorStyle]}>
                            <Text style={styles.detectionText}>Wake Word Detected!</Text>
                        </Animated.View>
                    )}
                </View>
            )}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        padding: 16,
        backgroundColor: '#f5f5f5',
        borderRadius: 8,
        marginBottom: 16,
    },
    row: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    label: {
        fontSize: 16,
        fontWeight: 'bold',
    },
    statusContainer: {
        marginTop: 8,
        alignItems: 'center',
    },
    statusText: {
        fontSize: 14,
        color: '#666',
    },
    detectionIndicator: {
        backgroundColor: '#2196F3',
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 20,
        marginTop: 8,
    },
    detectionText: {
        color: '#fff',
        fontWeight: 'bold',
    },
}); 