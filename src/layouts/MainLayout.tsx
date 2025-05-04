import React, { ReactNode } from 'react';
import { 
  View, 
  StyleSheet, 
  SafeAreaView, 
  StatusBar,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';

interface MainLayoutProps {
  children: ReactNode;
  scrollable?: boolean;
  padding?: boolean;
  keyboardAvoiding?: boolean;
}

const MainLayout: React.FC<MainLayoutProps> = ({
  children,
  scrollable = true,
  padding = true,
  keyboardAvoiding = true,
}) => {
  // Base component that handles SafeArea and StatusBar
  const BaseLayout = () => (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor="#fff" />
      
      {!scrollable && !keyboardAvoiding ? (
        <View style={[styles.container, padding && styles.padding]}>
          {children}
        </View>
      ) : null}
      
      {scrollable && !keyboardAvoiding ? (
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={[styles.scrollContent, padding && styles.padding]}
        >
          {children}
        </ScrollView>
      ) : null}
      
      {keyboardAvoiding ? (
        <KeyboardAvoidingView
          style={styles.keyboardAvoid}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
          {scrollable ? (
            <ScrollView 
              style={styles.scrollView}
              contentContainerStyle={[styles.scrollContent, padding && styles.padding]}
            >
              {children}
            </ScrollView>
          ) : (
            <View style={[styles.container, padding && styles.padding]}>
              {children}
            </View>
          )}
        </KeyboardAvoidingView>
      ) : null}
    </SafeAreaView>
  );

  return <BaseLayout />;
};

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#fff',
  },
  container: {
    flex: 1,
  },
  padding: {
    padding: 16,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
  },
  keyboardAvoid: {
    flex: 1,
  },
});

export default MainLayout; 