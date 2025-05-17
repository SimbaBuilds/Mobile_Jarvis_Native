import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import WakeWordService from './WakeWordService';
import { Alert } from 'react-native';
import { checkWakeWordPermissions, requestWakeWordPermissions } from '../settings/permissions';
import { VoiceState } from '../voice/VoiceService';
import { useVoiceState } from '../voice/hooks/useVoiceState';

interface WakeWordContextType {
    isEnabled: boolean;
    isRunning: boolean;
    setEnabled: (enabled: boolean) => Promise<void>;
    startDetection: () => Promise<void>;
    stopDetection: () => Promise<void>;
    onWakeWordDetected?: (timestamp: number) => void;
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
    const [isInitialized, setIsInitialized] = useState(false);
    const wakeWordService = WakeWordService.getInstance();
    
    // Get voice state to coordinate with ongoing conversations
    const { voiceState } = useVoiceState();

    // Sync state with native module
    const syncState = useCallback(async () => {
        try {
            const status = await wakeWordService.isWakeWordEnabled();
            console.log('🔄 Syncing wake word state:', status);
            
            // Only update state if it's different to avoid unnecessary re-renders
            if (isEnabled !== status) {
                console.log('📝 Updating enabled state:', status);
                setIsEnabled(status);
            }
            
            if (status) {
                const running = await wakeWordService.isWakeWordDetectionRunning();
                console.log('🔄 Syncing running state:', running);
                
                if (isRunning !== running) {
                    console.log('📝 Updating running state:', running);
                    setIsRunning(running);
                }
                
                if (!running && status) {
                    console.log('🔄 Auto-starting wake word detection');
                    await wakeWordService.startWakeWordDetection();
                    setIsRunning(true);
                }
            } else {
                // If not enabled, ensure running is false
                if (isRunning) {
                    console.log('📝 Setting running to false because enabled is false');
                    setIsRunning(false);
                }
            }
        } catch (error) {
            console.error('❌ Error syncing wake word state:', error);
        }
    }, [isEnabled, isRunning]);

    // Initialize state on mount
    useEffect(() => {
        let mounted = true;
        
        const initialize = async () => {
            try {
                // Initial state check
                const status = await wakeWordService.isWakeWordEnabled();
                console.log('🚀 Initial wake word state:', status);
                
                if (mounted) {
                    setIsEnabled(status);
                    
                    if (status) {
                        const running = await wakeWordService.isWakeWordDetectionRunning();
                        console.log('🚀 Initial running state:', running);
                        setIsRunning(running);
                        
                        if (!running) {
                            console.log('🚀 Starting wake word detection during initialization');
                            await wakeWordService.startWakeWordDetection();
                            setIsRunning(true);
                        }
                    }
                    
                    setIsInitialized(true);
                }
            } catch (error) {
                console.error('❌ Error during initialization:', error);
                if (mounted) {
                    setIsEnabled(false);
                    setIsRunning(false);
                    setIsInitialized(true);
                }
            }
        };
        
        initialize();
        
        return () => {
            mounted = false;
        };
    }, []);

    // Subscribe to wake word detection events
    useEffect(() => {
        const subscription = WakeWordService.addListener('wakeWordDetected', (event) => {
            const eventTime = event.timestamp ? new Date(event.timestamp) : new Date();
            const timeString = eventTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
            
            console.log('\n');
            console.log(`⏰ Time: ${timeString}, 🎤 WAKE WORD "JARVIS" DETECTED in React Native! 🎤`);
            
            // Skip wake word activation if not in IDLE state
            // More specific check using enum values rather than string comparison
            if (voiceState !== VoiceState.IDLE) {
                console.log('Conversation already in progress, ignoring wake word');
                return;
            }
            
            // Ensure running state is accurate
            setIsRunning(true);
        });

        return () => {
            subscription?.remove();
        };
    }, [voiceState]);

    // Periodically sync state with native module (every 30 seconds)
    useEffect(() => {
        if (!isInitialized) return;

        const interval = setInterval(syncState, 30000);
        return () => clearInterval(interval);
    }, [isInitialized, syncState]);

    const setEnabled = async (enabled: boolean) => {
        try {
            console.log('🔄 Setting wake word enabled:', enabled);
            
            if (enabled) {
                // Check permissions first
                const permissionsResult = await checkWakeWordPermissions();
                if (!permissionsResult.granted) {
                    console.log('📝 Need to request wake word permissions');
                    const requestResult = await requestWakeWordPermissions();
                    if (!requestResult.granted) {
                        console.error('❌ Permission request denied');
                        throw new Error('Microphone permission is required for wake word detection');
                    }
                }
            }
            
            const success = await wakeWordService.setWakeWordEnabled(enabled);
            
            if (success) {
                setIsEnabled(enabled);
                setIsRunning(enabled);
                console.log('✅ Wake word state updated successfully');
            } else {
                console.error('❌ Failed to set wake word enabled state');
                await syncState(); // Resync state on failure
            }
        } catch (error) {
            console.error('❌ Error setting wake word enabled state:', error);
            
            // Show user-friendly error message
            if (error instanceof Error && error.message.includes('permission')) {
                Alert.alert(
                    'Permission Required',
                    'Microphone permission is required for wake word detection. Please enable it in your device settings.',
                    [
                        { text: 'OK' }
                    ]
                );
            }
            
            await syncState(); // Resync state on error
            throw error; // Re-throw to let parent components handle it
        }
    };

    const startDetection = async () => {
        try {
            console.log('🎤 Starting wake word detection');
            await wakeWordService.startWakeWordDetection();
            setIsRunning(true);
            console.log('✅ Wake word detection started');
        } catch (error) {
            console.error('❌ Error starting wake word detection:', error);
            await syncState(); // Resync state on error
            throw error;
        }
    };

    const stopDetection = async () => {
        try {
            console.log('🛑 Stopping wake word detection');
            await wakeWordService.stopWakeWordDetection();
            setIsRunning(false);
            console.log('✅ Wake word detection stopped');
        } catch (error) {
            console.error('❌ Error stopping wake word detection:', error);
            await syncState(); // Resync state on error
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

    if (!isInitialized) {
        return null; // Or a loading indicator
    }

    return (
        <WakeWordContext.Provider value={value}>
            {children}
        </WakeWordContext.Provider>
    );
}; 