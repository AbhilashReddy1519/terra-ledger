/**
 * api/auth.js
 *
 * Calls the /auth/* endpoints (no /api prefix — see SecurityConfig).
 * Both endpoints return AuthResponse:
 *   { token, userId, email, role, walletAddress }
 */

import { post } from './client.js';

/**
 * Register a new user.
 * The frontend ALWAYS sends role: "CITIZEN" — officers/admins
 * are created via other means.
 *
 * @param {{ email: string, password: string }} credentials
 * @returns {Promise<AuthResponse>}
 */
export function register({ email, password }) {
  return post('/auth/register', { email, password, role: 'CITIZEN' });
}

/**
 * Log in with email + password.
 *
 * @param {{ email: string, password: string }} credentials
 * @returns {Promise<AuthResponse>}
 */
export function login({ email, password }) {
  return post('/auth/login', { email, password });
}
