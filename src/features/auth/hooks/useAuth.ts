import { useState, useEffect, useCallback } from 'react';
import { authService } from '../../../shared/services/authService';

interface UseAuthReturn {
  user: any | null;
  token: string | null;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string) => Promise<void>;
  logout: () => Promise<void>;
}

export const useAuth = (): UseAuthReturn => {
  const [user, setUser] = useState<any | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Check for existing token in storage on mount
  useEffect(() => {
    const checkToken = async () => {
      try {
        // In a real app, get token from secure storage
        const storedToken = null; // await SecureStore.getItemAsync('userToken');
        
        if (storedToken) {
          setIsLoading(true);
          const userData = await authService.verifyToken(storedToken);
          setUser(userData);
          setToken(storedToken);
        }
      } catch (err: any) {
        console.error('Token verification failed:', err);
        // Token is invalid, clear it
        // await SecureStore.deleteItemAsync('userToken');
      } finally {
        setIsLoading(false);
      }
    };
    
    checkToken();
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await authService.login(email, password);
      setUser(response.user);
      setToken(response.token);
      
      // In a real app, save token to secure storage
      // await SecureStore.setItemAsync('userToken', response.token);
    } catch (err: any) {
      setError(err.message || 'Login failed');
      console.error('Login error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (email: string, password: string, name: string) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await authService.register(email, password, name);
      setUser(response.user);
      setToken(response.token);
      
      // In a real app, save token to secure storage
      // await SecureStore.setItemAsync('userToken', response.token);
    } catch (err: any) {
      setError(err.message || 'Registration failed');
      console.error('Registration error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    setIsLoading(true);
    
    try {
      await authService.logout();
      setUser(null);
      setToken(null);
      
      // In a real app, remove token from secure storage
      // await SecureStore.deleteItemAsync('userToken');
    } catch (err: any) {
      console.error('Logout error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    user,
    token,
    isLoading,
    error,
    login,
    register,
    logout,
  };
}; 