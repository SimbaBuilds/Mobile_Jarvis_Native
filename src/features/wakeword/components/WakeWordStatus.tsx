import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useWakeWord } from '../context/WakeWordContext';

export const WakeWordStatus: React.FC = () => {
    const { isEnabled, isRunning } = useWakeWord();

    const getStatusColor = () => {
        if (!isEnabled) return '#666666';
        return isRunning ? '#4CAF50' : '#FFA000';
    };

    const getStatusText = () => {
        if (!isEnabled) return 'Wake Word Disabled';
        return isRunning ? 'Listening for "Jarvis"' : 'Wake Word Paused';
    };

    return (
        <View style={styles.container}>
            <View style={[styles.indicator, { backgroundColor: getStatusColor() }]} />
            <Text style={styles.text}>{getStatusText()}</Text>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 8,
        backgroundColor: '#1E1E1E',
        borderRadius: 8,
    },
    indicator: {
        width: 12,
        height: 12,
        borderRadius: 6,
        marginRight: 8,
    },
    text: {
        color: '#FFFFFF',
        fontSize: 14,
    },
}); 