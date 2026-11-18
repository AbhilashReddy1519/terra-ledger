/**
 * pages/SkeletonPage.jsx
 *
 * The original M0 walking-skeleton demo, now living at /skeleton.
 * No changes to the logic — just moved here and given a route.
 */

import { useState } from 'react';
import AppLayout from '../components/AppLayout.jsx';
import { useAuth } from '../context/AuthContext.jsx';

const API = '/api/skeleton';

export default function SkeletonPage() {
  const { isAuthenticated } = useAuth();
  const [record,  setRecord]  = useState(null);
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState(null);

  async function commitRecord() {
    setLoading(true); setError(null);
    try {
      const r = await fetch(`${API}/record`, { method: 'POST' });
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      setRecord(await r.json());
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function readRecord() {
    if (!record?.recordId) return;
    setLoading(true); setError(null);
    try {
      const r = await fetch(`${API}/record/${encodeURIComponent(record.recordId)}`);
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      setRecord(await r.json());
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  // Render inside AppLayout if authenticated, otherwise standalone
  const content = (
    <div style={{ maxWidth: 720, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>🌏 Terra Ledger — M0 Walking Skeleton</h1>
      <p style={{ color: 'var(--color-gray-500)', marginTop: '0.5rem' }}>
        This page demonstrates the full path: React → Spring Boot → (chain/mock) → back.
      </p>

      <div style={{ display: 'flex', gap: '1rem', margin: '1.5rem 0', flexWrap: 'wrap' }}>
        <button
          onClick={commitRecord}
          disabled={loading}
          className="btn btn-primary"
        >
          {loading ? 'Working…' : '1. Commit hardcoded record'}
        </button>
        <button
          onClick={readRecord}
          disabled={loading || !record?.recordId}
          className="btn"
          style={{ background: 'var(--color-success)', color: '#fff' }}
        >
          2. Read record back
        </button>
      </div>

      {error && (
        <pre style={{
          background: 'var(--color-danger-light)',
          border: '1px solid #fca5a5',
          padding: '1rem', borderRadius: 6, color: 'var(--color-danger)',
        }}>
          Error: {error}
        </pre>
      )}

      {record && (
        <div style={{
          background: 'var(--color-success-light)',
          border: '1px solid #86efac',
          padding: '1rem', borderRadius: 6,
        }}>
          <h3 style={{ marginTop: 0 }}>✅ Record from chain / API</h3>
          <pre style={{ margin: 0, overflowX: 'auto' }}>
            {JSON.stringify(record, null, 2)}
          </pre>
        </div>
      )}

      <footer style={{ marginTop: '3rem', color: 'var(--color-gray-400)', fontSize: 13 }}>
        M0 · Walking skeleton · moved to /skeleton route
      </footer>
    </div>
  );

  if (isAuthenticated) {
    return <AppLayout>{content}</AppLayout>;
  }

  return <div style={{ minHeight: '100vh', background: 'var(--color-gray-50)' }}>{content}</div>;
}
