import { NativeModules } from 'react-native';

const { SettingsModule } = NativeModules;

export interface ServerApiConfig {
  baseUrl: string;
  apiEndpoint: string;
}

export interface ApiKeys {
  picovoice: string;
  openai: string;
  deepgram: string;
  elevenlabs: string;
}

export interface AppConfig {
  serverApi: ServerApiConfig;
  apiKeys: ApiKeys;
}

/**
 * Service for interacting with app settings
 */
class SettingsService {
  /**
   * Get server API configuration
   */
  public async getServerApiConfig(): Promise<ServerApiConfig> {
    try {
      return await SettingsModule.getServerApiConfig();
    } catch (error) {
      console.error('Error getting server API config:', error);
      // Return default values on error
      return {
        baseUrl: 'http://192.168.1.131:8000',
        apiEndpoint: '/api/chat'
      };
    }
  }

  /**
   * Update server API configuration
   */
  public async updateServerApiConfig(config: Partial<ServerApiConfig>): Promise<boolean> {
    try {
      return await SettingsModule.updateServerApiConfig(
        config.baseUrl || '',
        config.apiEndpoint || ''
      );
    } catch (error) {
      console.error('Error updating server API config:', error);
      throw error;
    }
  }

  /**
   * Get full application configuration
   */
  public async getAppConfig(): Promise<AppConfig> {
    try {
      return await SettingsModule.getAppConfig();
    } catch (error) {
      console.error('Error getting app config:', error);
      throw error;
    }
  }
}

// Export singleton instance
export default new SettingsService(); 