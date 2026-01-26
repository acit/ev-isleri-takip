import { create } from 'zustand';

interface AuthState {
  token: string | null;
  userId: number | null;
  familyId: number | null;
  isAuthenticated: boolean;
  setAuth: (token: string, userId: number, familyId: number) => void;
  clearAuth: () => void;
  checkAuth: () => boolean;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: localStorage.getItem('token'),
  userId: localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')!) : null,
  familyId: localStorage.getItem('familyId') ? parseInt(localStorage.getItem('familyId')!) : null,
  isAuthenticated: !!localStorage.getItem('token'),
  
  setAuth: (token: string, userId: number, familyId: number) => {
    localStorage.setItem('token', token);
    localStorage.setItem('userId', userId.toString());
    localStorage.setItem('familyId', familyId.toString());
    set({ token, userId, familyId, isAuthenticated: true });
  },
  
  clearAuth: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('familyId');
    set({ token: null, userId: null, familyId: null, isAuthenticated: false });
  },
  
  checkAuth: () => {
    const token = localStorage.getItem('token');
    const isAuth = !!token;
    set({ isAuthenticated: isAuth });
    return isAuth;
  }
}));
