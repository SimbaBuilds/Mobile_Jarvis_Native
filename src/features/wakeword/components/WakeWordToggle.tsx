import React from 'react';
import { View, Switch, Text, StyleSheet } from 'react-native';
import { useWakeWord } from '../WakeWordContext';

export const WakeWordToggle: React.FC = () => {
    const { isEnabled, setEnabled } = useWakeWord();

    const handleToggle = async (value: boolean) => {
        try {
            await setEnabled(value);
        } catch (error) {
            console.error('Error toggling wake word:', error);
        }
    };

    return (
        <View style={styles.container}>
            <Text style={styles.label}>Wake Word Detection</Text>
            <Switch
                value={isEnabled}
                onValueChange={handleToggle}
                trackColor={{ false: '#767577', true: '#81b0ff' }}
                thumbColor={isEnabled ? '#2196F3' : '#f4f3f4'}
            />
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: 16,
        backgroundColor: '#1E1E1E',
        borderRadius: 8,
        marginVertical: 8,
    },
    label: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '500',
    },
}); 