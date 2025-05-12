import { useState, useCallback, useEffect } from 'react';
import ServerApiService, { ServerApiConfig, ChatResponse } from './ServerApiService';
import { ChatMessage } from '../features/voice/VoiceContext';

/**
 * Return type for the useServerApi hook
 */
interface UseServerApiResult {
  isLoading: boolean;
  error: Error | null;
  response: ChatResponse | null;
  sendMessage: (message: string, history: ChatMessage[]) => Promise<ChatResponse>;
  updateConfig: (config: Partial<ServerApiConfig>) => void;
}

/**
 * Options for the useServerApi hook
 */
interface UseServerApiOptions {
  initialConfig?: Partial<ServerApiConfig>;
  preferences?: {
    voice?: string;
    response_type?: string;
    [key: string]: any;
  };
  onResponse?: (response: ChatResponse) => void;
  onError?: (error: Error) => void;
}

/**
 * Hook for interacting with the server API
 */
export const useServerApi = (options: UseServerApiOptions = {}): UseServerApiResult => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [response, setResponse] = useState<ChatResponse | null>(null);

  // Initialize with custom config if provided
  useEffect(() => {
    if (options.initialConfig) {
      ServerApiService.updateConfig(options.initialConfig);
    }
  }, []);

  /**
   * Send message to server API
   */
  const sendMessage = useCallback(async (
    message: string, 
    history: ChatMessage[]
  ): Promise<ChatResponse> => {
    setIsLoading(true);
    setError(null);

    try {
      const result = await ServerApiService.sendChatRequest(
        message, 
        history, 
        options.preferences
      );
      
      setResponse(result);
      setIsLoading(false);
      
      // Call onResponse callback if provided
      if (options.onResponse) {
        options.onResponse(result);
      }
      
      return result;
    } catch (err) {
      const error = err instanceof Error ? err : new Error(String(err));
      setError(error);
      setIsLoading(false);
      
      // Call onError callback if provided
      if (options.onError) {
        options.onError(error);
      }
      
      throw error;
    }
  }, [options.preferences, options.onResponse, options.onError]);

  /**
   * Update server API configuration
   */
  const updateConfig = useCallback((config: Partial<ServerApiConfig>): void => {
    ServerApiService.updateConfig(config);
  }, []);

  return {
    isLoading,
    error,
    response,
    sendMessage,
    updateConfig
  };
}; 