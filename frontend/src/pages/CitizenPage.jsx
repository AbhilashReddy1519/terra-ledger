/**
 * pages/CitizenPage.jsx
 *
 * Dashboard for CITIZEN role (also accessible to OFFICER and ADMIN).
 * Calls GET /api/citizen/dashboard.
 *
 * Shows: welcome message from backend, loading state, error state.
 */

import { useEffect, useState } from 'react';
import AppLayout from '../components/AppLayout.jsx';
import Card from '../components/Card.jsx';
import Spinner from '../components/Spinner.jsx';
import { getCitizenDashboard } from '../api/dashboard.js';

export default function CitizenPage() {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    let cancelled = false;

    getCitizenDashboard()
      .then((res) => { if (!cancelled) setData(res); })
      .catch((err) => { if (!cancelled) setError(err.message ?? 'Failed to load dashboard.'); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, []);

  return (
    <AppLayout>
      <div className="dashboard-welcome">
        <h2>Citizen Dashboard</h2>
        <p>Upload land records and track their review status.</p>
      </div>

      {loading && <Spinner size="lg" center />}

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}

      {data && (
        <Card title="Welcome">
          <p>{data.message}</p>
          <p style={{ marginTop: '0.75rem', color: 'var(--color-gray-500)', fontSize: 'var(--font-size-sm)' }}>
            Signed in as <strong>{data.email}</strong> · Role: <strong>{data.role}</strong>
          </p>
        </Card>
      )}
    </AppLayout>
  );
}
