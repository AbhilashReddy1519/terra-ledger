/**
 * api/dashboard.js
 *
 * Calls the /api/{role}/dashboard endpoints.
 * All three require a valid JWT (attached automatically by client.js).
 *
 * Each returns:
 *   { view: string, email: string, role: string, message: string }
 */

import { get } from './client.js';

export const getCitizenDashboard = () => get('/api/citizen/dashboard');
export const getOfficerDashboard = () => get('/api/officer/dashboard');
export const getAdminDashboard   = () => get('/api/admin/dashboard');
