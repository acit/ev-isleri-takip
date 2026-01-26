import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

export const apiClient = axios.create({
  baseURL: API_URL,
});

// Add token to requests
apiClient.interceptors.request.use((config: any) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  (response: any) => response,
  (error: any) => {
    if (error.response?.status === 401) {
      // Clear auth on unauthorized
      useAuthStore.getState().clearAuth();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  sendLoginCode: (email: string) => apiClient.post('/auth/login-code', { email }),
  verifyCode: (email: string, code: string) => apiClient.post('/auth/verify-code', { email, code }),
  loginWithPassword: (email: string, password: string) => apiClient.post('/auth/login', { email, password }),
  resetPassword: (email: string) => apiClient.post('/auth/reset-password', { email }),
  confirmPasswordReset: (email: string, code: string, newPassword: string) => 
    apiClient.post('/auth/confirm-reset', { email, code, newPassword }),
  setPassword: (currentPassword: string, newPassword: string) => 
    apiClient.post('/auth/set-password', { currentPassword, newPassword }),
  getEmailStatus: () => apiClient.get('/auth/email-status'),
  inviteFamily: (email: string) => apiClient.post('/auth/invite', { email }),
  acceptInvite: (token: string, email: string) => apiClient.post('/auth/accept-invite', { token, email }),
};

export const taskAPI = {
  create: (data: any) => apiClient.post('/tasks', data),
  getAll: (params?: any) => apiClient.get('/tasks', { params }),
  getByFrequency: () => apiClient.get('/tasks/by-frequency'),
  complete: (taskId: number) => apiClient.patch(`/tasks/${taskId}/complete`),
  update: (taskId: number, data: any) => apiClient.patch(`/tasks/${taskId}`, data),
};

export const inventoryAPI = {
  add: (data: any) => apiClient.post('/inventory', data),
  getAll: () => apiClient.get('/inventory'),
  getLowStock: () => apiClient.get('/inventory/low-stock'),
  update: (itemId: number, data: any) => apiClient.patch(`/inventory/${itemId}`, data),
  delete: (itemId: number) => apiClient.delete(`/inventory/${itemId}`),
};

export const shoppingAPI = {
  create: (data: any) => apiClient.post('/shopping', data),
  getAll: () => apiClient.get('/shopping'),
  getItems: (listId: number) => apiClient.get(`/shopping/${listId}/items`),
  updateItem: (itemId: number, data: any) => apiClient.patch(`/shopping/items/${itemId}`, data),
  generateFromInventory: () => apiClient.post('/shopping/generate-from-inventory', {}),
  sendByEmail: (listId: number, emails: string[]) => 
    apiClient.post(`/shopping/${listId}/send-email`, { recipientEmails: emails }),
};

export const budgetAPI = {
  get: (params?: any) => apiClient.get('/budget', { params }),
  setLimit: (data: any) => apiClient.post('/budget/set-limit', data),
  recordExpense: (data: any) => apiClient.post('/budget/expenses', data),
  getExpenses: (params?: any) => apiClient.get('/budget/expenses', { params }),
};

export const messagesAPI = {
  send: (data: any) => apiClient.post('/messages', data),
  getAll: (params?: any) => apiClient.get('/messages', { params }),
  getFamilyMembers: () => apiClient.get('/messages/family-members'),
  getUnreadCount: () => apiClient.get('/messages/unread-count'),
  markAsRead: (messageId: number) => apiClient.patch(`/messages/${messageId}/read`),
};
