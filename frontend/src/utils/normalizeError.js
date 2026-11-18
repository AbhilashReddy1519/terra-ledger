/**
 * utils/normalizeError.js
 *
 * The backend throws two error shapes:
 *   1. { error, message }                  — from SecurityConfig's handlers (401/403)
 *   2. { timestamp, status, error,         — Spring's ResponseStatusException
 *          message, path }
 *
 * This helper always returns: { status: number, message: string }
 */

/**
 * @param {Response} response - The raw fetch Response
 * @returns {Promise<{ status: number, message: string }>}
 */
export async function normalizeError(response) {
  let body = null;

  try {
    const text = await response.text();
    if (text) {
      body = JSON.parse(text);
    }
  } catch {
    // body wasn't JSON — fall through to generic message
  }

  const status = response.status;

  if (body) {
    // Both Spring error shapes have a "message" field — use it.
    if (typeof body.message === 'string' && body.message) {
      return { status, message: body.message };
    }
    // Fallback: use the "error" field
    if (typeof body.error === 'string' && body.error) {
      return { status, message: body.error };
    }
  }

  // Generic HTTP fallback
  return { status, message: `Request failed with status ${status}` };
}

/**
 * Wraps a network-level error (fetch threw, e.g. server unreachable).
 * @param {unknown} err
 * @returns {{ status: 0, message: string }}
 */
export function normalizeNetworkError(err) {
  const message =
    err instanceof Error ? err.message : 'Network error – please check your connection.';
  return { status: 0, message };
}
