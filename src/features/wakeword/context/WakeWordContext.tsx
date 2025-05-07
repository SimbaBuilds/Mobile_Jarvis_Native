import React, { createContext, useContext, useState, useEffect } from 'react';
import WakeWordService from '../services/WakeWordService';

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
                setIsEnabled(enabled);
                
                if (enabled) {
                    const running = await wakeWordService.isWakeWordDetectionRunning();
                    setIsRunning(running);
                }
            } catch (error) {
                console.error('Error initializing wake word:', error);
            }
        };

        initializeWakeWord();
    }, []);

    const setEnabled = async (enabled: boolean) => {
        try {
            await wakeWordService.setWakeWordEnabled(enabled);
            setIsEnabled(enabled);
            setIsRunning(enabled);
        } catch (error) {
            console.error('Error setting wake word enabled state:', error);
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