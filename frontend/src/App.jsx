/**
 * App.jsx  — Root router
 *
 * Routes:
 *   /              → redirects to /login (or role home if already logged in)
 *   /login         → LoginPage
 *   /register      → RegisterPage
 *   /citizen       → CitizenPage   (CITIZEN | OFFICER | ADMIN)
 *   /officer       → OfficerPage   (OFFICER | ADMIN)
 *   /admin         → AdminPage     (ADMIN only)
 *   /skeleton      → SkeletonPage  (M0 demo, no auth required)
 *   *              → NotFoundPage
 */

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';

import LoginPage    from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import CitizenPage  from './pages/CitizenPage.jsx';
import OfficerPage  from './pages/OfficerPage.jsx';
import AdminPage    from './pages/AdminPage.jsx';
import SkeletonPage from './pages/SkeletonPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import ProtectedRoute from './routes/ProtectedRoute.jsx';

// Redirect logged-in users away from the root to their role home
const ROLE_HOME = {
  CITIZEN: '/citizen',
  OFFICER: '/officer',
  ADMIN:   '/admin',
};

function RootRedirect() {
  const { isAuthenticated, role } = useAuth();
  if (isAuthenticated && role && ROLE_HOME[role]) {
    return <Navigate to={ROLE_HOME[role]} replace />;
  }
  return <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/"         element={<RootRedirect />} />
        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/skeleton" element={<SkeletonPage />} />

        {/* Protected – role-gated */}
        <Route
          path="/citizen"
          element={
            <ProtectedRoute allowedRoles={['CITIZEN', 'OFFICER', 'ADMIN']}>
              <CitizenPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/officer"
          element={
            <ProtectedRoute allowedRoles={['OFFICER', 'ADMIN']}>
              <OfficerPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminPage />
            </ProtectedRoute>
          }
        />

        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
