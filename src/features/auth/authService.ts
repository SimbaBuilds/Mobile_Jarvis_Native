// This is a mock authentication service
// In a real app, this would make API calls to your backend

interface User {
  id: string;
  email: string;
  name: string;
}

interface LoginResponse {
  user: User;
  token: string;
}

interface AuthError {
  message: string;
  code?: string;
}

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export const authService = {
  /**
   * Simulate a login API call
   */
  login: async (email: string, password: string): Promise<LoginResponse> => {
    // Simulate API delay
    await delay(1000);
    
    // Simulate validation
    if (!email || !password) {
      throw { message: 'Email and password are required' };
    }
    
    // For demo purposes, accept any email with a valid format and password longer than 5 chars
    if (!/\S+@\S+\.\S+/.test(email)) {
      throw { message: 'Invalid email format', code: 'INVALID_EMAIL' };
    }
    
    if (password.length < 6) {
      throw { message: 'Password must be at least 6 characters', code: 'INVALID_PASSWORD' };
    }
    
    // Simulate successful login
    return {
      user: {
        id: '1',
        email,
        name: email.split('@')[0], // Just use part of email as name for demo
      },
      token: 'mock-jwt-token-' + Math.random().toString(36).substring(2),
    };
  },
  
  /**
   * Simulate a registration API call
   */
  register: async (
    email: string, 
    password: string, 
    name: string
  ): Promise<LoginResponse> => {
    // Simulate API delay
    await delay(1500);
    
    // Validation similar to login
    if (!email || !password || !name) {
      throw { message: 'All fields are required' };
    }
    
    if (!/\S+@\S+\.\S+/.test(email)) {
      throw { message: 'Invalid email format', code: 'INVALID_EMAIL' };
    }
    
    if (password.length < 6) {
      throw { message: 'Password must be at least 6 characters', code: 'INVALID_PASSWORD' };
    }
    
    if (name.length < 2) {
      throw { message: 'Name is too short', code: 'INVALID_NAME' };
    }
    
    // Simulate successful registration
    return {
      user: {
        id: '1',
        email,
        name,
      },
      token: 'mock-jwt-token-' + Math.random().toString(36).substring(2),
    };
  },
  
  /**
   * Simulate a logout API call
   */
  logout: async (): Promise<void> => {
    await delay(500);
    // In a real app, you would invalidate the token on the server
    return;
  },
  
  /**
   * Verify if token is valid
   */
  verifyToken: async (token: string): Promise<User> => {
    await delay(500);
    
    if (!token || !token.startsWith('mock-jwt-token-')) {
      throw { message: 'Invalid token', code: 'INVALID_TOKEN' };
    }
    
    // Simulate getting user from token
    return {
      id: '1',
      email: 'user@example.com',
      name: 'User',
    };
  },
}; 