/**
 * main.jsx — Application entry point
 *
 * Wraps the whole app in AuthProvider so every component
 * can access auth state via useAuth().
 *
 * CSS imports order:
 *  1. tokens.css  — CSS variables & global reset
 *  2. components.css — reusable component classes
 */

import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles/tokens.css';
import './styles/components.css';
import { AuthProvider } from './context/AuthContext.jsx';
import App from './App.jsx';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>
);
