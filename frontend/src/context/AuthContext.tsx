import React, { createContext, useContext, useEffect, useState } from 'react';
import { api, unwrap } from '../services/api';
import { UserProfile } from '../types';

interface AuthCtx {
  user: UserProfile | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<UserProfile>;
  register: (payload: { email: string; password: string; fullName: string; phone?: string; accountType?: string }) => Promise<UserProfile>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthCtx | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = async () => {
    try {
      const res = await api.get('/auth/me');
      setUser(unwrap<UserProfile>(res));
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshUser();
  }, []);

  const login = async (email: string, password: string) => {
    const res = await api.post('/auth/login', { email, password });
    const data = unwrap<{ user: UserProfile; accessToken: string }>(res);
    if (data.accessToken) sessionStorage.setItem('mc_access_token', data.accessToken);
    setUser(data.user);
    return data.user;
  };

  const register = async (payload: { email: string; password: string; fullName: string; phone?: string; accountType?: string }) => {
    const res = await api.post('/auth/register', payload);
    const data = unwrap<{ user: UserProfile; accessToken: string }>(res);
    if (data.accessToken) sessionStorage.setItem('mc_access_token', data.accessToken);
    setUser(data.user);
    return data.user;
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      sessionStorage.removeItem('mc_access_token');
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
