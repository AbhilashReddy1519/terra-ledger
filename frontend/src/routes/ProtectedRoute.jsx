/**
 * routes/ProtectedRoute.jsx
 *
 * Wraps any route that needs:
 *   1. Authentication – redirects to /login if not logged in.
 *   2. Role authorization – shows a "Not Authorized" page if the
 *      user's role isn't in the allowed list.
 *
 * Usage:
 *   <ProtectedRoute allowedRoles={['ADMIN']}>
 *     <AdminPage />
 *   </ProtectedRoute>
 *
 * When no allowedRoles is passed, any authenticated user may access the route.
 */

import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  // 1. Not logged in → redirect to /login, preserving the intended URL
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 2. Role not allowed → show friendly error page (no redirect, stays on URL)
  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(role)) {
    return <NotAuthorized userRole={role} allowedRoles={allowedRoles} />;
  }

  return children;
}

// ── Inline "Not Authorized" display ───────────────────────────────
function NotAuthorized({ userRole, allowedRoles }) {
  return (
    <div className="error-page">
      <div className="error-page-code">403</div>
      <h1 className="error-page-title">Not Authorized</h1>
      <p className="error-page-desc">
        Your role <strong>{userRole}</strong> does not have access to this page.
        {allowedRoles?.length > 0 && (
          <> This area is restricted to: {allowedRoles.join(', ')}.</>
        )}
      </p>
      <a href="/" className="btn btn-primary" style={{ marginTop: '1rem' }}>
        Go to Home
      </a>
    </div>
  );
}
