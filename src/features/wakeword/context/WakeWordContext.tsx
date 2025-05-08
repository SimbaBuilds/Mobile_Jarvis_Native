import React, { createContext, useContext, useState, useEffect } from 'react';
import WakeWordService from '../../../shared/services/NativeModules/WakeWordService';

interface WakeWordContextType {
    isEnabled: boolean;
    isRunning: boolean;
    setEnabled: (enabled: boolean) => Promise<void>;
    startDetection: () => Promise<void>;
    stopDetection: () => Promise<void>;
}

const WakeWordContext = createContext<WakeWordContextType | null>(null);

export const useWakeWord = () => {
    const context = useContext(WakeWordContext);
    if (!context) {
        throw new Error('useWakeWord must be used within a WakeWordProvider');
    }
    return context;
};

export const WakeWordProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isEnabled, setIsEnabled] = useState(false);
    const [isRunning, setIsRunning] = useState(false);
    const wakeWordService = WakeWordService.getInstance();

    useEffect(() => {
        const initializeWakeWord = async () => {
            try {
                const enabled = await wakeWordService.isWakeWordEnabled();
                console.log('Initial wake word enabled state:', enabled);
                setIsEnabled(enabled);
                
                if (enabled) {
                    // If enabled, check if it's actually running
                    const running = await wakeWordService.isWakeWordDetectionRunning();
                    console.log('Initial wake word running state:', running);
                    setIsRunning(running);
                    
                    // If enabled but not running, start detection
                    if (!running) {
                        await wakeWordService.startWakeWordDetection();
                        setIsRunning(true);
                    }
                }
            } catch (error) {
                console.error('Error initializing wake word:', error);
                setIsEnabled(false);
                setIsRunning(false);
            }
        };

        initializeWakeWord();
    }, []);

    const setEnabled = async (enabled: boolean) => {
        try {
            const success = await wakeWordService.setWakeWordEnabled(enabled);
            console.log('Set wake word enabled result:', success);
            
            if (success) {
                setIsEnabled(enabled);
                setIsRunning(enabled);
            } else {
                console.error('Failed to set wake word enabled state');
                // Revert to previous state if operation failed
                const currentState = await wakeWordService.isWakeWordEnabled();
                setIsEnabled(currentState);
                setIsRunning(currentState);
            }
        } catch (error) {
            console.error('Error setting wake word enabled state:', error);
            // On error, revert to previous state
            const currentState = await wakeWordService.isWakeWordEnabled();
            setIsEnabled(currentState);
            setIsRunning(currentState);
            throw error;
        }
    };

    const startDetection = async () => {
        try {
            await wakeWordService.startWakeWordDetection();
            setIsRunning(true);
        } catch (error) {
            console.error('Error starting wake word detection:', error);
            throw error;
        }
    };

    const stopDetection = async () => {
        try {
            await wakeWordService.stopWakeWordDetection();
            setIsRunning(false);
        } catch (error) {
            console.error('Error stopping wake word detection:', error);
            throw error;
        }
    };

    const value = {
        isEnabled,
        isRunning,
        setEnabled,
        startDetection,
        stopDetection,
    };

    return (
        <WakeWordContext.Provider value={value}>
            {children}
        </WakeWordContext.Provider>
    );
}; 