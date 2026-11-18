/**
 * tests/normalizeError.test.js
 *
 * Unit tests for the error-normalizing utility.
 * No DOM, no React — pure JS logic.
 */

import { describe, it, expect } from 'vitest';
import { normalizeError, normalizeNetworkError } from '../utils/normalizeError.js';

// Helper: build a fake Response with a JSON body
function fakeResponse(status, body) {
  const json = JSON.stringify(body);
  return new Response(json, {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

// Helper: build a fake Response with a plain-text (non-JSON) body
function fakeTextResponse(status, text) {
  return new Response(text, {
    status,
    headers: { 'Content-Type': 'text/plain' },
  });
}

describe('normalizeError', () => {
  it('extracts message from SecurityConfig 401/403 shape { error, message }', async () => {
    const res = fakeResponse(401, {
      error: 'Unauthorized',
      message: 'Full authentication is required',
    });
    const err = await normalizeError(res);
    expect(err.status).toBe(401);
    expect(err.message).toBe('Full authentication is required');
  });

  it('extracts message from Spring ResponseStatusException shape', async () => {
    const res = fakeResponse(409, {
      timestamp: '2024-01-01T00:00:00Z',
      status: 409,
      error: 'Conflict',
      message: 'Email is already registered',
      path: '/auth/register',
    });
    const err = await normalizeError(res);
    expect(err.status).toBe(409);
    expect(err.message).toBe('Email is already registered');
  });

  it('falls back to the error field when message is absent', async () => {
    const res = fakeResponse(403, { error: 'Forbidden' });
    const err = await normalizeError(res);
    expect(err.status).toBe(403);
    expect(err.message).toBe('Forbidden');
  });

  it('falls back to generic message for non-JSON bodies', async () => {
    const res = fakeTextResponse(500, 'Internal Server Error');
    const err = await normalizeError(res);
    expect(err.status).toBe(500);
    expect(err.message).toMatch(/500/);
  });

  it('falls back to generic message when body is empty', async () => {
    const res = new Response('', { status: 503 });
    const err = await normalizeError(res);
    expect(err.status).toBe(503);
    expect(err.message).toMatch(/503/);
  });
});

describe('normalizeNetworkError', () => {
  it('wraps a native Error', () => {
    const err = normalizeNetworkError(new Error('fetch failed'));
    expect(err.status).toBe(0);
    expect(err.message).toBe('fetch failed');
  });

  it('wraps a non-Error value', () => {
    const err = normalizeNetworkError('some string');
    expect(err.status).toBe(0);
    expect(err.message).toMatch(/network error/i);
  });
});
