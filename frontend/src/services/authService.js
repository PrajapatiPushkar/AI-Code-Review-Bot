import api from './api';

export const authService = {
  async login(usernameOrEmail, password) {
    const response = await api.post('/auth/login', {
      usernameOrEmail,
      password
    });
    
    if (response.data && response.data.accessToken) {
      localStorage.setItem('token', response.data.accessToken);
      // Store basic user identity
      const userObj = { usernameOrEmail };
      localStorage.setItem('user', JSON.stringify(userObj));
    }
    return response.data;
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getToken() {
    return localStorage.getItem('token');
  },

  getUser() {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  isAuthenticated() {
    return !!localStorage.getItem('token');
  }
};

export default authService;
