/**
 * tests/ProtectedRoute.test.jsx
 *
 * Tests for the ProtectedRoute component:
 *  1. Unauthenticated → redirects to /login
 *  2. Authenticated but wrong role → shows "Not Authorized"
 *  3. Authenticated + correct role → renders children
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProtectedRoute from '../routes/ProtectedRoute.jsx';
import * as AuthContextModule from '../context/AuthContext.jsx';

// ── Helper: render ProtectedRoute at a given path with mocked auth ───
function renderProtected({ isAuthenticated, role, allowedRoles = ['ADMIN'] }) {
  // Mock useAuth to return controlled values
  vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
    isAuthenticated,
    role,
    email: 'test@example.com',
    logout: vi.fn(),
  });

  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route
          path="/protected"
          element={
            <ProtectedRoute allowedRoles={allowedRoles}>
              <div>Secret Content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  it('redirects unauthenticated users to /login', () => {
    renderProtected({ isAuthenticated: false, role: null });
    // Should land on the /login route
    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });

  it('shows Not Authorized for wrong role', () => {
    renderProtected({
      isAuthenticated: true,
      role: 'CITIZEN',
      allowedRoles: ['ADMIN'],
    });
    expect(screen.getByText(/Not Authorized/i)).toBeInTheDocument();
    expect(screen.getByText(/403/)).toBeInTheDocument();
  });

  it('renders children for an allowed role', () => {
    renderProtected({
      isAuthenticated: true,
      role: 'ADMIN',
      allowedRoles: ['ADMIN'],
    });
    expect(screen.getByText('Secret Content')).toBeInTheDocument();
  });

  it('renders children when no allowedRoles restriction is set', () => {
    renderProtected({
      isAuthenticated: true,
      role: 'CITIZEN',
      allowedRoles: [],   // empty = no role restriction
    });
    expect(screen.getByText('Secret Content')).toBeInTheDocument();
  });
});
