import { useState } from 'react'

const API = '/api/skeleton'

export default function App() {
  const [record, setRecord]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)

  async function commitRecord() {
    setLoading(true); setError(null)
    try {
      const r = await fetch(`${API}/record`, { method: 'POST' })
      if (!r.ok) throw new Error(`HTTP ${r.status}`)
      const data = await r.json()
      setRecord(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  async function readRecord() {
    if (!record?.recordId) return
    setLoading(true); setError(null)
    try {
      const r = await fetch(`${API}/record/${encodeURIComponent(record.recordId)}`)
      if (!r.ok) throw new Error(`HTTP ${r.status}`)
      const data = await r.json()
      setRecord(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ fontFamily: 'sans-serif', maxWidth: 720, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>🌏 Terra Ledger — M0 Walking Skeleton</h1>
      <p style={{ color: '#555' }}>
        This page demonstrates the full path: React → Spring Boot → (chain/mock) → back.
      </p>

      <div style={{ display: 'flex', gap: '1rem', margin: '1.5rem 0' }}>
        <button onClick={commitRecord} disabled={loading}
          style={btnStyle('#2563eb')}>
          {loading ? 'Working…' : '1. Commit hardcoded record'}
        </button>
        <button onClick={readRecord} disabled={loading || !record?.recordId}
          style={btnStyle('#16a34a')}>
          2. Read record back
        </button>
      </div>

      {error && (
        <pre style={{ background: '#fef2f2', border: '1px solid #fca5a5',
                      padding: '1rem', borderRadius: 6, color: '#dc2626' }}>
          Error: {error}
        </pre>
      )}

      {record && (
        <div style={{ background: '#f0fdf4', border: '1px solid #86efac',
                      padding: '1rem', borderRadius: 6 }}>
          <h3 style={{ marginTop: 0 }}>✅ Record from chain / API</h3>
          <pre style={{ margin: 0, overflowX: 'auto' }}>
            {JSON.stringify(record, null, 2)}
          </pre>
        </div>
      )}

      <footer style={{ marginTop: '3rem', color: '#9ca3af', fontSize: 13 }}>
        M0 · Walking skeleton · no real auth or chain write yet
      </footer>
    </div>
  )
}

function btnStyle(bg) {
  return {
    background: bg, color: '#fff', border: 'none', padding: '0.6rem 1.2rem',
    borderRadius: 6, cursor: 'pointer', fontSize: 15, fontWeight: 600
  }
}
