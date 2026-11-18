/**
 * pages/OfficerPage.jsx
 *
 * Dashboard for OFFICER role (also accessible to ADMIN).
 * Calls GET /api/officer/dashboard.
 */

import { useEffect, useState } from 'react';
import AppLayout from '../components/AppLayout.jsx';
import Card from '../components/Card.jsx';
import Spinner from '../components/Spinner.jsx';
import { getOfficerDashboard } from '../api/dashboard.js';

export default function OfficerPage() {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    let cancelled = false;

    getOfficerDashboard()
      .then((res) => { if (!cancelled) setData(res); })
      .catch((err) => { if (!cancelled) setError(err.message ?? 'Failed to load dashboard.'); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, []);

  return (
    <AppLayout>
      <div className="dashboard-welcome">
        <h2>Officer Dashboard</h2>
        <p>Review submitted land records and approve or reject them.</p>
      </div>

      {loading && <Spinner size="lg" center />}

      {error && (
        <div className="alert alert-error" role="alert">
          {error}
        </div>
      )}

      {data && (
        <Card title="Welcome, Revenue Officer">
          <p>{data.message}</p>
          <p style={{ marginTop: '0.75rem', color: 'var(--color-gray-500)', fontSize: 'var(--font-size-sm)' }}>
            Signed in as <strong>{data.email}</strong> · Role: <strong>{data.role}</strong>
          </p>
        </Card>
      )}
    </AppLayout>
  );
}
