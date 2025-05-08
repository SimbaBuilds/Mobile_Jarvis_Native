import React, { useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { VoiceProvider } from './src/features/voice/VoiceContext';
import { WakeWordProvider } from './src/features/wakeword/WakeWordContext';
import WakeWordService from './src/features/wakeword/WakeWordService';
import { HomeScreen } from './src/HomeScreen';
import { SettingsScreen } from './src/features/settings/SettingsScreen';
import { Ionicons } from '@expo/vector-icons';

type RootStackParamList = {
  Home: undefined;
  Settings: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  // Initialize any needed configurations here
  useEffect(() => {
    // Initialize app settings
    const initializeApp = async () => {
      try {
        // Check if wake word detection is available
        const isAvailable = await WakeWordService.getInstance().isWakeWordEnabled();
        console.log(`Wake word detection available: ${isAvailable}`);
      } catch (error) {
        console.error('Error initializing app:', error);
      }
    };
    
    initializeApp();
  }, []);

  return (
    <NavigationContainer>
      <VoiceProvider>
        <WakeWordProvider>
          <Stack.Navigator
            screenOptions={{
              headerStyle: {
                backgroundColor: '#f5f5f5',
              },
              headerTintColor: '#333',
              headerTitleStyle: {
                fontWeight: 'bold',
              },
            }}
          >
            <Stack.Screen 
              name="Home" 
              component={HomeScreen} 
              options={({ navigation }) => ({
                title: 'Jarvis',
                headerRight: () => (
                  <Ionicons 
                    name="settings-outline" 
                    size={24} 
                    color="#333"
                    style={{ marginRight: 16 }}
                    onPress={() => navigation.navigate('Settings')}
                  />
                ),
              })}
            />
            <Stack.Screen 
              name="Settings" 
              component={SettingsScreen}
              options={{
                title: 'Settings',
              }}
            />
          </Stack.Navigator>
        </WakeWordProvider>
      </VoiceProvider>
    </NavigationContainer>
  );
}
