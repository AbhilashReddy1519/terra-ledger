/**
 * pages/LoginPage.jsx
 *
 * Email + password login form.
 *
 * Validates:
 *  - Valid email format
 *  - Non-empty password
 *
 * On success → redirects by role:
 *  CITIZEN  → /citizen
 *  OFFICER  → /officer
 *  ADMIN    → /admin
 *
 * Shows session-expired message when ?expired=1 is in the URL.
 */

import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import Input from '../components/Input.jsx';
import Button from '../components/Button.jsx';

// Map roles to their home routes
const ROLE_HOME = {
  CITIZEN: '/citizen',
  OFFICER: '/officer',
  ADMIN:   '/admin',
};

function validateForm({ email, password }) {
  const errors = {};
  if (!email) {
    errors.email = 'Email is required.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = 'Please enter a valid email address.';
  }
  if (!password) {
    errors.password = 'Password is required.';
  }
  return errors;
}

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [email,    setEmail]    = useState('');
  const [password, setPassword] = useState('');
  const [errors,   setErrors]   = useState({});
  const [apiError, setApiError] = useState('');
  const [loading,  setLoading]  = useState(false);

  const sessionExpired = searchParams.get('expired') === '1';

  async function handleSubmit(e) {
    e.preventDefault();

    // Client-side validation
    const fieldErrors = validateForm({ email, password });
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length > 0) return;

    setApiError('');
    setLoading(true);

    try {
      const session = await login({ email, password });
      const dest = ROLE_HOME[session.role] ?? '/';
      navigate(dest, { replace: true });
    } catch (err) {
      // err is always { status, message } from our API layer
      setApiError(err.message ?? 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>🌏 Terra Ledger</h1>
          <p>Sign in to your account</p>
        </div>

        {/* Session expired banner */}
        {sessionExpired && (
          <div className="alert alert-error" style={{ marginBottom: '1.25rem' }} role="alert">
            ⚠️ Your session expired. Please log in again.
          </div>
        )}

        {/* API error banner */}
        {apiError && (
          <div className="alert alert-error" style={{ marginBottom: '1.25rem' }} role="alert">
            {apiError}
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <Input
            id="email"
            label="Email address"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={errors.email}
            placeholder="you@example.com"
            autoComplete="email"
            disabled={loading}
          />

          <Input
            id="password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
            placeholder="••••••••"
            autoComplete="current-password"
            disabled={loading}
          />

          <Button type="submit" full loading={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>

        <p className="auth-form-footer">
          Don't have an account?{' '}
          <Link to="/register">Create one</Link>
        </p>
      </div>
    </div>
  );
}
