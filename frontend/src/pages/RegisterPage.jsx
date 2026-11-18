/**
 * pages/RegisterPage.jsx
 *
 * Self-registration for CITIZEN role only.
 * The role field is NEVER shown to the user and is always sent as "CITIZEN".
 *
 * Validates:
 *  - Valid email format
 *  - Password min 6 chars (matches backend @Size(min=6))
 *  - Confirm password must match
 *
 * On success → auto-logs in and redirects to /citizen.
 */

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import Input from '../components/Input.jsx';
import Button from '../components/Button.jsx';

function validateForm({ email, password, confirmPassword }) {
  const errors = {};

  if (!email) {
    errors.email = 'Email is required.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = 'Please enter a valid email address.';
  }

  if (!password) {
    errors.password = 'Password is required.';
  } else if (password.length < 6) {
    errors.password = 'Password must be at least 6 characters.';
  }

  if (!confirmPassword) {
    errors.confirmPassword = 'Please confirm your password.';
  } else if (password !== confirmPassword) {
    errors.confirmPassword = 'Passwords do not match.';
  }

  return errors;
}

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [email,           setEmail]           = useState('');
  const [password,        setPassword]        = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors,          setErrors]          = useState({});
  const [apiError,        setApiError]        = useState('');
  const [loading,         setLoading]         = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();

    const fieldErrors = validateForm({ email, password, confirmPassword });
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length > 0) return;

    setApiError('');
    setLoading(true);

    try {
      // register() in AuthContext always sends role: 'CITIZEN'
      const session = await register({ email, password });
      // After successful registration, the backend logs us in and returns a token.
      // Citizens always go to /citizen.
      navigate(session.role === 'CITIZEN' ? '/citizen' : '/', { replace: true });
    } catch (err) {
      setApiError(err.message ?? 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>🌏 Terra Ledger</h1>
          <p>Create a citizen account</p>
        </div>

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
            placeholder="At least 6 characters"
            autoComplete="new-password"
            disabled={loading}
          />

          <Input
            id="confirmPassword"
            label="Confirm password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            error={errors.confirmPassword}
            placeholder="Repeat your password"
            autoComplete="new-password"
            disabled={loading}
          />

          <Button type="submit" full loading={loading}>
            {loading ? 'Creating account…' : 'Create account'}
          </Button>
        </form>

        <p className="auth-form-footer">
          Already have an account?{' '}
          <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
