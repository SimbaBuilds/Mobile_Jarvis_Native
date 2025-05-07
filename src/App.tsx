import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { VoiceProvider } from './features/voice/context/VoiceContext';
import { VoiceErrorBoundary } from './components/ErrorBoundary/VoiceErrorBoundary';
import { VoiceAssistant } from './components/VoiceAssistant/VoiceAssistant';
import { WakeWordToggle } from './features/voice/components/WakeWordToggle';
import { usePermissions } from './hooks/usePermissions';
import { Ionicons } from '@expo/vector-icons';

type RootStackParamList = {
  Home: undefined;
  Settings: undefined;
};

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

const Stack = createNativeStackNavigator<RootStackParamList>();

const HomeScreen = ({ navigation }: { navigation: NavigationProp }) => {
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Mobile Jarvis Native</Text>
        <Text style={styles.subtitle}>Your personal mobile assistant</Text>
      </View>
      
      <VoiceErrorBoundary>
        <View style={styles.voiceAssistantContainer}>
          <VoiceAssistant onSpeechResult={(text) => console.log('Speech recognized:', text)} />
        </View>
      </VoiceErrorBoundary>
    </View>
  );
};

const SettingsScreen = () => {
  const {
    permissions,
    loading,
    requestMicrophone,
    requestBatteryExemption,
    hasMicrophonePermission,
    hasBatteryOptimizationExemption,
  } = usePermissions();

  if (loading) {
    return (
      <View style={[styles.container, styles.centerContent]}>
        <Text>Loading permissions...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Voice Settings</Text>
        <WakeWordToggle label="Wake Word Detection (Jarvis)" />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Permissions</Text>
        <View style={styles.permissionItem}>
          <Text style={styles.permissionTitle}>Microphone Access</Text>
          <Text style={styles.permissionStatus}>
            Status: {hasMicrophonePermission ? '✅ Granted' : '❌ Not Granted'}
          </Text>
          {!hasMicrophonePermission && (
            <Text 
              style={styles.permissionButton}
              onPress={requestMicrophone}
            >
              Grant Microphone Permission
            </Text>
          )}
        </View>
      </View>
    </View>
  );
};

export default function App() {
  return (
    <NavigationContainer>
      <VoiceProvider>
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
        <StatusBar style="auto" />
      </VoiceProvider>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    padding: 16,
  },
  centerContent: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    marginVertical: 24,
    alignItems: 'center',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333',
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginTop: 8,
  },
  voiceAssistantContainer: {
    flex: 1,
    marginVertical: 16,
  },
  section: {
    marginVertical: 16,
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 16,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 16,
    color: '#333',
  },
  permissionItem: {
    marginVertical: 8,
  },
  permissionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  permissionStatus: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  permissionButton: {
    color: '#007AFF',
    marginTop: 8,
    fontSize: 14,
    fontWeight: '500',
  },
});
