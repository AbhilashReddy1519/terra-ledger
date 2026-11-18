/**
 * context/AuthContext.jsx
 *
 * Provides global auth state:
 *   { token, userId, email, role, walletAddress }
 *
 * Persists to localStorage under 'tl_session'.
 * Restores state on page refresh.
 * Exposes: login, register, logout, isAuthenticated.
 */

import { createContext, useContext, useState, useCallback } from 'react';
import * as authApi from '../api/auth.js';

const SESSION_KEY = 'tl_session';

// ── Helper: load saved session ───────────────────────────────────
function loadSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

// ── Default context shape ─────────────────────────────────────────
const AuthContext = createContext(null);

// ── Provider ──────────────────────────────────────────────────────
export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => loadSession());

  /**
   * Persist a successful auth response and update state.
   * @param {AuthResponse} authResponse
   */
  const persist = useCallback((authResponse) => {
    const data = {
      token:         authResponse.token,
      userId:        authResponse.userId,
      email:         authResponse.email,
      role:          authResponse.role,          // 'CITIZEN' | 'OFFICER' | 'ADMIN'
      walletAddress: authResponse.walletAddress ?? null,
    };
    localStorage.setItem(SESSION_KEY, JSON.stringify(data));
    setSession(data);
    return data;
  }, []);

  /**
   * Register then auto-login (backend returns a token on register too).
   * @param {{ email: string, password: string }} credentials
   * @returns {Promise<{ role: string }>}
   */
  const register = useCallback(async (credentials) => {
    const authResponse = await authApi.register(credentials);
    return persist(authResponse);
  }, [persist]);

  /**
   * Email + password login.
   * @param {{ email: string, password: string }} credentials
   * @returns {Promise<{ role: string }>}
   */
  const login = useCallback(async (credentials) => {
    const authResponse = await authApi.login(credentials);
    return persist(authResponse);
  }, [persist]);

  /**
   * Clear local session.
   */
  const logout = useCallback(() => {
    localStorage.removeItem(SESSION_KEY);
    setSession(null);
  }, []);

  const value = {
    // Current session fields (null when logged out)
    token:         session?.token         ?? null,
    userId:        session?.userId        ?? null,
    email:         session?.email         ?? null,
    role:          session?.role          ?? null,
    walletAddress: session?.walletAddress ?? null,

    isAuthenticated: Boolean(session?.token),

    // Actions
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// ── Hook ──────────────────────────────────────────────────────────
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
