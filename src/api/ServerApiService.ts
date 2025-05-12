import { ChatMessage } from '../features/voice/VoiceContext';
import SettingsService from '../settings/SettingsService';

// Default server configuration
const DEFAULT_SERVER_CONFIG = {
  baseUrl: 'http://192.168.1.131:8000',
  apiEndpoint: '/api/chat'
};

/**
 * Options for server API configuration
 */
export interface ServerApiConfig {
  baseUrl: string;
  apiEndpoint: string;
}

/**
 * Request payload for chat API
 */
export interface ChatRequest {
  message: string;
  timestamp: number;
  history: ChatMessage[];
  preferences?: {
    voice?: string;
    response_type?: string;
    [key: string]: any;
  };
}

/**
 * Response from chat API
 */
export interface ChatResponse {
  response: string;
  timestamp: number;
  additional_data?: any;
}

/**
 * Service for handling API calls to server
 */
class ServerApiService {
  private config: ServerApiConfig;

  constructor(config?: Partial<ServerApiConfig>) {
    this.config = {
      ...DEFAULT_SERVER_CONFIG,
      ...config
    };
    console.log('ServerApiService initialized with config:', this.config);
    
    // Load configuration from settings module
    this.loadConfig();
  }
  
  /**
   * Load configuration from native settings
   */
  private async loadConfig(): Promise<void> {
    try {
      const serverConfig = await SettingsService.getServerApiConfig();
      this.updateConfig(serverConfig);
      console.log('Loaded server config from native settings:', serverConfig);
    } catch (error) {
      console.error('Error loading server config from native settings:', error);
    }
  }

  /**
   * Update service configuration
   */
  public updateConfig(config: Partial<ServerApiConfig>): void {
    this.config = {
      ...this.config,
      ...config
    };
    console.log('ServerApiService config updated:', this.config);
  }

  /**
   * Send chat request to server
   */
  public async sendChatRequest(
    message: string,
    history: ChatMessage[],
    preferences?: ChatRequest['preferences']
  ): Promise<ChatResponse> {
    console.log(`Sending chat request to ${this.config.baseUrl}${this.config.apiEndpoint}`);
    
    try {
      const request: ChatRequest = {
        message,
        timestamp: Date.now(),
        history,
        preferences: preferences || {
          voice: 'male',
          response_type: 'concise'
        }
      };

      console.log('Request payload:', JSON.stringify(request));

      const response = await fetch(`${this.config.baseUrl}${this.config.apiEndpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('Server API error:', response.status, errorText);
        throw new Error(`Server API error: ${response.status} ${errorText}`);
      }

      const data: ChatResponse = await response.json();
      console.log('Server response:', data);
      return data;
    } catch (error) {
      console.error('Error sending chat request:', error);
      throw error;
    }
  }
}

// Export singleton instance
export default new ServerApiService(); 