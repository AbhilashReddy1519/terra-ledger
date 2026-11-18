/**
 * api/client.js
 *
 * Thin fetch wrapper that:
 *  - Reads the base URL from VITE_API_BASE_URL env var (defaults to '')
 *    so the Vite proxy handles /api and /auth in dev, and nginx handles them
 *    in Docker.
 *  - Attaches Authorization: Bearer <token> when a session token exists.
 *  - Sends JSON by default; passes FormData through without forcing
 *    a Content-Type (the browser sets it with the correct boundary).
 *  - Normalizes all errors into { status, message }.
 *  - On a 401 from an *authenticated* call it clears localStorage,
 *    then redirects to /login?expired=1.
 */

import { normalizeError, normalizeNetworkError } from '../utils/normalizeError.js';

// Key used by AuthContext to persist the session
const SESSION_KEY = 'tl_session';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

/**
 * Returns the stored token (or null).
 */
function getToken() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    return JSON.parse(raw)?.token ?? null;
  } catch {
    return null;
  }
}

/**
 * Core request function.
 *
 * @param {string} path       - e.g. '/auth/login', '/api/citizen/dashboard'
 * @param {RequestInit} [options]
 * @returns {Promise<any>}    - Parsed JSON body on success
 * @throws {{ status: number, message: string }} on any error
 */
export async function request(path, options = {}) {
  const token = getToken();
  const isAuthenticated = Boolean(token);

  const headers = new Headers(options.headers ?? {});

  // Attach bearer token when available
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  // Default to JSON unless sending FormData
  if (!(options.body instanceof FormData)) {
    if (!headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }
  }

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers,
    });
  } catch (err) {
    throw normalizeNetworkError(err);
  }

  // Session-expired guard: 401 on an authenticated call → force logout
  if (response.status === 401 && isAuthenticated) {
    localStorage.removeItem(SESSION_KEY);
    window.location.href = '/login?expired=1';
    // Throw so the calling code doesn't continue
    throw { status: 401, message: 'Session expired. Please log in again.' };
  }

  if (!response.ok) {
    throw await normalizeError(response);
  }

  // 204 No Content
  if (response.status === 204) return null;

  // Parse JSON (some endpoints might return plain text in edge cases)
  try {
    return await response.json();
  } catch {
    return null;
  }
}

/**
 * Convenience helpers
 */
export const get  = (path, opts) => request(path, { method: 'GET',    ...opts });
export const post = (path, body, opts) =>
  request(path, {
    method: 'POST',
    body: body instanceof FormData ? body : JSON.stringify(body),
    ...opts,
  });
export const put  = (path, body, opts) =>
  request(path, {
    method: 'PUT',
    body: body instanceof FormData ? body : JSON.stringify(body),
    ...opts,
  });
export const del  = (path, opts) => request(path, { method: 'DELETE', ...opts });
