/**
 * components/AppLayout.jsx
 *
 * Top navbar + page wrapper for authenticated views.
 *
 * Shows:
 *  - App name "Terra Ledger"
 *  - Logged-in user's email and role pill
 *  - Logout button
 */

import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function AppLayout({ children }) {
  const { email, role, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <>
      <nav className="navbar" aria-label="Main navigation">
        <div className="navbar-inner">
          {/* Brand */}
          <a href="/" className="navbar-brand">
            Terra <span>Ledger</span>
          </a>

          {/* User info + logout */}
          <div className="navbar-user">
            {email && (
              <span className="navbar-email" title={email}>
                {email}
              </span>
            )}
            {role && <span className="navbar-role">{role}</span>}
            <button
              className="navbar-logout"
              onClick={handleLogout}
              type="button"
            >
              Logout
            </button>
          </div>
        </div>
      </nav>

      <main className="page-content">{children}</main>
    </>
  );
}
